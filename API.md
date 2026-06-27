# Wasteland API

Wasteland now exposes a public API for external plugins such as CurseFeatures.

## Loading

In the external plugin's `plugin.yml`, depend on Wasteland so the provider is ready:

```yml
depend:
  - Wasteland
```

Then access the API:

```java
import dev.r3faced.minecurse.wasteland.api.WastelandApi;
import dev.r3faced.minecurse.wasteland.api.WastelandProvider;

WastelandApi api = WastelandProvider.get();
```

The API is also registered through Bukkit services as `WastelandApi`.

## Common Mask Hooks

### XP multiplier

```java
@EventHandler
public void onWastelandXp(WastelandXpGainEvent event) {
    if (!hasMask(event.getPlayer(), "miner")) return;

    event.setAmount(Math.round(event.getAmount() * 1.50D));
}
```

### Cancel or bypass tier-locked ores

```java
@EventHandler
public void onTierLockedOre(WastelandTierLockedOreBreakEvent event) {
    if (!hasMask(event.getPlayer(), "ore_bypass")) return;

    // Cancelling this API event bypasses the Wasteland tier lock for this break.
    event.setCancelled(true);
}
```

### Add bonus rewards

```java
WastelandApi api = WastelandProvider.get();
api.addStoredReward(player, customReward);
```

## Main API Areas

- Player data: `getPlayer`, `getPlayerData`, `getLevel`, `getXp`, `getTier`, `getPlaytimeSeconds`
- Progress changes: `addXp`, `removeXp`, `setXp`, `setLevel`, `setTier`, `resetPlayer`
- Tiers and rewards: `getRequiredLevel`, `meetsTierRequirements`, `getTierRewards`, `addStoredReward`, `claimStoredReward`
- Tools: `buildOmniTool`, `giveOmniTool`, `isOmniTool`, `getToolSkill`
- Worlds: `isWastelandWorld`, `getSkillForWorld`, `getWastelandWorlds`
- GUIs: `openMainMenu`, `openStatsMenu`, `openSkillMenu`, `openTierMenu`, `openTierRewardsMenu`, `openCollectMenu`, `openHelpMenu`

## Events

- `WastelandXpGainEvent`: cancellable and amount-editable before XP is applied.
- `WastelandSkillChangeEvent`: fired after XP or level changes.
- `WastelandSkillLevelUpEvent`: fired after XP causes a level-up.
- `WastelandTierUnlockEvent`: cancellable before a tier unlock is applied.
- `WastelandTierChangeEvent`: fired after the shared tier changes.
- `WastelandStoredRewardAddEvent`: cancellable and reward-editable before a stored reward is added.
- `WastelandStoredRewardClaimEvent`: cancellable before stored reward commands run.
- `WastelandTierLockedOreBreakEvent`: cancellable to bypass a mining tier lock.
- `WastelandToolGiveEvent`: cancellable and item-editable before an Omni Tool is given.
- `WastelandPlayerResetEvent`: cancellable before progress is reset.
