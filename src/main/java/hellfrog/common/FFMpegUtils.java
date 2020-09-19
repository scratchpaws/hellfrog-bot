package hellfrog.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FFMpegUtils {

    private FFMpegUtils() {
        throw new RuntimeException("Cannot be created");
    }

    private static final Pattern DURATION_PATTERN = Pattern.compile("\\d\\d:\\d\\d:\\d\\d\\.\\d\\d");
    private static final Pattern DURATION_SEARCH = Pattern.compile("Duration: \\d\\d:\\d\\d:\\d\\d\\.\\d\\d",
            Pattern.MULTILINE);
    private static final Logger log = LogManager.getLogger(FFMpegUtils.class.getSimpleName());

    public static FFMpegDuration getMediaDuration(@NotNull final Path mediaFile) throws IOException {
        if (Files.notExists(mediaFile)) {
            throw new IOException("file not found: " + mediaFile);
        }

        String procOutput = "<no proc output>";
        final List<String> cmdline = List.of("ffmpeg", "-hide_banner", "-i", mediaFile.toString());
        try {
            final Process process = new ProcessBuilder()
                    .command(cmdline)
                    .start();
            final ProcessOutputBuffer stdout = new ProcessOutputBuffer(process.getInputStream());
            final ProcessOutputBuffer stderr = new ProcessOutputBuffer(process.getErrorStream());
            process.waitFor(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);

            procOutput = stdout.getOutput() + stderr.getOutput();

            final Matcher durationsFound = DURATION_SEARCH.matcher(procOutput);
            if (!durationsFound.find()) {
                String errMsg = String.format("ffmpeg does not return duration of \"%s\"", mediaFile.toString());
                throw new IOException(errMsg);
            }
            final String rawDurationSubstring = durationsFound.group();
            final Matcher durationsLength = DURATION_PATTERN.matcher(rawDurationSubstring);
            if (!durationsLength.find()) {
                String errMsg = String.format("ffmpeg does not return duration of \"%s\"", mediaFile.toString());
                throw new IOException(errMsg);
            }
            final String rawDurationValue = durationsLength.group();
            return FFMpegDuration.parseDuration(rawDurationValue);
        } catch (IOException err) {
            String errMsg = String.format("cannot execute ffmpeg for run \"%s\": %s",
                    cmdline.toString(), err.getMessage());
            log.error(errMsg, err);
            log.error(procOutput);
            throw new IOException(errMsg, err);
        } catch (InterruptedException err) {
            String errMsg = String.format("to long ffmpeg execution of \"%s\"", cmdline.toString());
            log.error(errMsg, err);
            log.error(procOutput);
            throw new IOException(errMsg, err);
        }
    }

    public static Path mergeVideo(@NotNull final Path inputVideo,
                                  @NotNull final Path inputAudio,
                                  @NotNull final FFMpegDuration duration) throws IOException {
        if (Files.notExists(inputAudio) || Files.notExists(inputVideo)) {
            throw new IOException("files not found: " + inputAudio + ", " + inputVideo);
        }

        final Path resultFile = Files.createTempFile(CodeSourceUtils.getCodeSourceParent(), "combined_", ".mp4");

        String procOutput = "<no proc output>";
        final List<String> cmdline = List.of("ffmpeg", "-hide_banner", "-y", "-i", inputVideo.toString(),
                "-i", inputAudio.toString(), "-c", "copy", "-t", duration.toString(), resultFile.toString());

        try {
            final Process process = new ProcessBuilder()
                    .command(cmdline)
                    .start();
            final ProcessOutputBuffer stdout = new ProcessOutputBuffer(process.getInputStream());
            final ProcessOutputBuffer stderr = new ProcessOutputBuffer(process.getErrorStream());
            process.waitFor(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);

            procOutput = stdout.getOutput() + stderr.getOutput();
            if (process.exitValue() != 0) {
                String errMsg = String.format("cannot merge video and audio \"%s\" \"%s\": ffmpeg returned: %d",
                        inputVideo, inputAudio, process.exitValue());
                throw new IOException(errMsg);
            }
        } catch (IOException err) {
            String errMsg = String.format("cannot execute ffmpeg for run \"%s\": %s",
                    cmdline.toString(), err.getMessage());
            log.error(errMsg, err);
            log.error(procOutput);
            throw new IOException(errMsg, err);
        } catch (InterruptedException err) {
            String errMsg = String.format("to long ffmpeg execution of \"%s\"", cmdline.toString());
            log.error(errMsg, err);
            log.error(procOutput);
            throw new IOException(errMsg, err);
        }

        if (Files.size(resultFile) == 0L) {
            String errMsg = String.format("cannot merge video and audio \"%s\" \"%s\": output file has zero size",
                    inputVideo, inputAudio);
            log.error(errMsg);
            log.error(procOutput);
            throw new IOException(errMsg);
        }

        return resultFile;
    }
}
