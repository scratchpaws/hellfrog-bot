package hellfrog.common;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class DiscordImage extends InMemoryAttach {

    private static final byte[] ZERO_DATA = new byte[0];
    private static final String EMPTY_EXT = "";
    private static final DiscordImage EMPTY_AVATAR = new DiscordImage(ZERO_DATA, EMPTY_EXT, null);
    private final URI imageURI;

    private DiscordImage(final byte[] imageData, final String extension, final URI imageURI) {
        super(extension, imageData);
        this.imageURI = imageURI;
    }

    public static DiscordImage ofBytesWithExt(final byte[] imageData, final String extension) {
        return new DiscordImage(imageData, extension, null);
    }

    public static DiscordImage ofImageURI(final URI imageURI) {
        return new DiscordImage(ZERO_DATA, EMPTY_EXT, imageURI);
    }

    public static DiscordImage ofEmpty() {
        return EMPTY_AVATAR;
    }

    public byte[] getImageData() {
        return super.getBytes();
    }

    public String getExtension() {
        return super.getFileName();
    }

    public URI getImageURI() {
        return imageURI;
    }

    public void setImage(@NotNull final EmbedBuilder embedBuilder) {
        if (imageURI == null
                && super.getBytes() != null && super.getBytes().length > 0
                && super.getFileName() != null && super.getFileName().length() > 0) {

            embedBuilder.setImage(super.getBytes(), super.getFileName());

        } else if (imageURI == null && super.getBytes() != null && super.getBytes().length > 0) {
            embedBuilder.setImage(super.getBytes());
        } else if (imageURI != null) {
            embedBuilder.setImage(imageURI.toString());
        }
    }

    public void setThumbnail(@NotNull final EmbedBuilder embedBuilder) {
        if (imageURI == null
                && super.getBytes() != null && super.getBytes().length > 0
                && super.getFileName() != null && super.getFileName().length() > 0) {

            embedBuilder.setThumbnail(super.getBytes(), super.getFileName());

        } else if (imageURI == null && super.getBytes() != null && super.getBytes().length > 0) {
            embedBuilder.setThumbnail(super.getBytes());
        } else if (imageURI != null) {
            embedBuilder.setThumbnail(imageURI.toString());
        }
    }
}
