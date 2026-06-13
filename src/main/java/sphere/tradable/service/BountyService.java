// File: src/main/java/sphere/tradable/service/BountyService.java
package sphere.tradable.service;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import sphere.tradable.model.BountyEntry;
import sphere.tradable.model.SortMode;
import sphere.tradable.storage.BountyStorage;
import sphere.tradable.util.MoneyUtil;
import sphere.tradable.util.PlayerRefResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class BountyService {
    private final BountyStorage bountyStorage;

    public BountyService(final BountyStorage bountyStorage) {
        this.bountyStorage = Objects.requireNonNull(bountyStorage, "bountyStorage");
    }

    public BountyEntry addBounty(final Player setter, final OfflinePlayer target, final double amount) {
        return addBounty((Object) setter, (Object) target, amount);
    }

    public BountyEntry addBounty(final OfflinePlayer setter, final OfflinePlayer target, final double amount) {
        return addBounty((Object) setter, (Object) target, amount);
    }

    public BountyEntry addBounty(final UUID setterId, final UUID targetId, final double amount) {
        return addBounty((Object) setterId, (Object) targetId, amount);
    }

    public BountyEntry addBounty(final String setterName, final String targetName, final double amount) {
        return addBounty((Object) setterName, (Object) targetName, amount);
    }

    public BountyEntry addBounty(final Object setterRef, final Object targetRef, final double amount) {
        final UUID setterId = PlayerRefResolver.requireUuid(setterRef);
        final UUID targetId = PlayerRefResolver.requireUuid(targetRef);
        final double value = MoneyUtil.requirePositive(amount, "Amount");

        if (setterId.equals(targetId)) {
            throw new IllegalArgumentException("You cannot place a bounty on yourself.");
        }

        final BountyEntry entry = new BountyEntry(
                setterId,
                targetId,
                value,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        bountyStorage.addEntry(entry);
        return entry;
    }

    public BountyEntry createBounty(final Player setter, final OfflinePlayer target, final double amount) {
        return addBounty(setter, target, amount);
    }

    public BountyEntry createBounty(final UUID setterId, final UUID targetId, final double amount) {
        return addBounty(setterId, targetId, amount);
    }

    public BountyEntry createBounty(final String setterName, final String targetName, final double amount) {
        return addBounty(setterName, targetName, amount);
    }

    public BountyEntry setBounty(final Player setter, final OfflinePlayer target, final double amount) {
        return addBounty(setter, target, amount);
    }

    public BountyEntry setBounty(final UUID setterId, final UUID targetId, final double amount) {
        return addBounty(setterId, targetId, amount);
    }

    public BountyEntry setBounty(final String setterName, final String targetName, final double amount) {
        return addBounty(setterName, targetName, amount);
    }

    public List<BountyEntry> getBounties() {
        return getBounties(SortMode.HIGHEST);
    }

    public List<BountyEntry> getBounties(final SortMode sortMode) {
        final SortMode effectiveSort = sortMode == null ? SortMode.HIGHEST : sortMode;
        final List<BountyEntry> entries = new ArrayList<>(bountyStorage.getEntries());
        entries.sort(effectiveSort.bountyComparator());
        return entries;
    }

    public List<BountyEntry> getEntries() {
        return getBounties();
    }

    public List<BountyEntry> getEntries(final SortMode sortMode) {
        return getBounties(sortMode);
    }

    public List<BountyEntry> getBounties(final String rawSortMode) {
        return getBounties(SortMode.fromString(rawSortMode));
    }

    public List<BountyEntry> getBountiesForTarget(final OfflinePlayer target, final SortMode sortMode) {
        Objects.requireNonNull(target, "target");
        return getBountiesForTarget(target.getUniqueId(), sortMode);
    }

    public List<BountyEntry> getBountiesForTarget(final UUID targetId, final SortMode sortMode) {
        Objects.requireNonNull(targetId, "targetId");

        final SortMode effectiveSort = sortMode == null ? SortMode.HIGHEST : sortMode;
        final List<BountyEntry> result = new ArrayList<>();

        for (final BountyEntry entry : bountyStorage.getEntries()) {
            if (targetId.equals(entry.getTargetId())) {
                result.add(entry);
            }
        }

        result.sort(effectiveSort.bountyComparator());
        return result;
    }

    public List<BountyEntry> getBountiesForTarget(final String targetName, final SortMode sortMode) {
        return getBountiesForTarget(PlayerRefResolver.requireUuid(targetName), sortMode);
    }

    public double getTotalBounty(final OfflinePlayer target) {
        Objects.requireNonNull(target, "target");
        return getTotalBounty(target.getUniqueId());
    }

    public double getTotalBounty(final UUID targetId) {
        Objects.requireNonNull(targetId, "targetId");
        return MoneyUtil.sanitize(bountyStorage.getTotalBounty(targetId));
    }

    public double getTotalBounty(final String targetName) {
        return getTotalBounty(PlayerRefResolver.requireUuid(targetName));
    }

    public boolean hasBounty(final OfflinePlayer target) {
        return getTotalBounty(target) > 0D;
    }

    public boolean hasBounty(final UUID targetId) {
        return getTotalBounty(targetId) > 0D;
    }

    public boolean hasBounty(final String targetName) {
        return getTotalBounty(targetName) > 0D;
    }

    public int removeBounties(final OfflinePlayer target) {
        Objects.requireNonNull(target, "target");
        return removeBounties(target.getUniqueId());
    }

    public int removeBounties(final UUID targetId) {
        Objects.requireNonNull(targetId, "targetId");
        return bountyStorage.removeByTarget(targetId);
    }

    public int removeBounties(final String targetName) {
        return removeBounties(PlayerRefResolver.requireUuid(targetName));
    }
}