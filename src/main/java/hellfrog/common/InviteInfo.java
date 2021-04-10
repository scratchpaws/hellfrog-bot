package hellfrog.common;

import org.javacord.api.entity.server.invite.RichInvite;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InviteInfo {

    private final long inviterId;
    private final String code;
    private final int usagesCount;
    private final int maxUsages;
    private final Instant expiredDate;

    private InviteInfo(final long inviterId,
                       final String code,
                       final int usagesCount,
                       final int maxUsages,
                       final @Nullable Instant expiredDate) {
        this.inviterId = inviterId;
        this.code = code;
        this.usagesCount = usagesCount;
        this.maxUsages = maxUsages;
        this.expiredDate = expiredDate;
    }

    @Contract("_ -> new")
    @NotNull
    public static InviteInfo fromRichInvite(@NotNull final RichInvite richInvite) {
        final Instant expiredDate = richInvite.getMaxAgeInSeconds() > 0
                ? richInvite.getCreationTimestamp().plusSeconds(richInvite.getMaxAgeInSeconds())
                : null;
        return new InviteInfo(richInvite.getInviter().map(User::getId).orElse(0L),
                richInvite.getCode(),
                richInvite.getUses(),
                richInvite.getMaxUses(),
                expiredDate);
    }

    @NotNull @UnmodifiableView
    public static List<InviteInfo> fromServerInvites(@NotNull final Collection<RichInvite> richInvites) {
        final List<InviteInfo> result = new ArrayList<>(richInvites.size());
        for (RichInvite richInvite : richInvites) {
            result.add(fromRichInvite(richInvite));
        }
        return Collections.unmodifiableList(result);
    }

    public long getInviterId() {
        return inviterId;
    }

    public String getCode() {
        return code;
    }

    public int getUsagesCount() {
        return usagesCount;
    }

    public int getMaxUsages() {
        return maxUsages;
    }

    @Nullable
    public Instant getExpiredDate() {
        return expiredDate;
    }

    @Override
    public String toString() {
        return "InviteInfo{" +
                "inviterId=" + inviterId +
                ", code='" + code + '\'' +
                ", usagesCount=" + usagesCount +
                ", maxUsages=" + maxUsages +
                ", expiredDate=" + expiredDate +
                '}';
    }
}
