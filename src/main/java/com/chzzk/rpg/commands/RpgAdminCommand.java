package com.chzzk.rpg.commands;

import com.chzzk.rpg.ChzzkRPG;
import com.chzzk.rpg.cube.CubeType;
import com.chzzk.rpg.grade.BonusStat;
import com.chzzk.rpg.grade.BonusStatGenerator;
import com.chzzk.rpg.grade.WeaponGrade;
import com.chzzk.rpg.items.ItemManager;
import com.chzzk.rpg.items.WeaponData;
import com.chzzk.rpg.jobs.LifeJob;
import com.chzzk.rpg.stats.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RpgAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "help", "reload", "giveweapon", "givescroll", "givegradestone", "givecube",
        "givemoney", "setgrade", "weaponinfo", "setjob", "setblacksmithlv",
        "rerollstats", "testkit", "setpoints"
    );

    private static final List<String> GRADES = Arrays.asList(
        "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"
    );

    private static final List<String> CUBE_TYPES = Arrays.asList("BASIC", "ADVANCED");

    private static final List<String> JOBS = Arrays.asList("NONE", "BLACKSMITH", "CHEF", "BUILDER");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("rpg.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            ChzzkRPG.getInstance().reloadConfig();
            sender.sendMessage("§aConfig reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setpoints")) {
            // /rpgadmin setpoints <player> <amount>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin setpoints <player> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[2]);
                PlayerProfile profile = ChzzkRPG.getInstance().getStatsManager().getProfile(target);
                if (profile != null) {
                    profile.setStatPoints(amount);
                    profile.recalculateStats();
                    ChzzkRPG.getInstance().getStatsManager().saveProfile(target.getUniqueId());
                    sender.sendMessage("§aSet " + target.getName() + "'s stat points to " + amount);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("giveweapon")) {
            // /rpgadmin giveweapon <player> <atk>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin giveweapon <player> <atk>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            try {
                double atk = Double.parseDouble(args[2]);
                org.bukkit.inventory.ItemStack item = com.chzzk.rpg.items.ItemManager
                        .createWeapon(org.bukkit.Material.DIAMOND_SWORD, "§bStarter Sword", atk,
                                target.getUniqueId());
                target.getInventory().addItem(item);
                sender.sendMessage("§aGiven weapon with " + atk + " ATK to " + target.getName());
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("givescroll")) {
            // /rpgadmin givescroll <player> <type>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin givescroll <player> <type>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            String type = args[2].toUpperCase();
            org.bukkit.inventory.ItemStack scroll = com.chzzk.rpg.items.ItemManager
                    .createScroll("§a강화 주문서 (" + type + ")", type);
            target.getInventory().addItem(scroll);
            sender.sendMessage("§aGiven scroll to " + target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("givegradestone")) {
            // /rpgadmin givegradestone <player> [amount]
            if (args.length < 2) {
                sender.sendMessage("Usage: /rpgadmin givegradestone <player> [amount]");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number.");
                    return true;
                }
            }

            ItemStack stone = com.chzzk.rpg.items.ItemManager.createGradeStone(amount);
            target.getInventory().addItem(stone);
            sender.sendMessage("§aGiven " + amount + " grade stone(s) to " + target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("givecube")) {
            // /rpgadmin givecube <player> <BASIC|ADVANCED> [amount]
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin givecube <player> <BASIC|ADVANCED> [amount]");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            CubeType cubeType;
            try {
                cubeType = CubeType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid cube type. Use BASIC or ADVANCED.");
                return true;
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number.");
                    return true;
                }
            }

            ItemStack cube = com.chzzk.rpg.items.ItemManager.createCube(cubeType, amount);
            target.getInventory().addItem(cube);
            sender.sendMessage("§aGiven " + amount + " " + cubeType.getDisplayName() + "(s) to " + target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("setgrade")) {
            // /rpgadmin setgrade <grade>
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cPlayers only.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("Usage: /rpgadmin setgrade <COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC>");
                return true;
            }

            Player player = (Player) sender;
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (!WeaponData.isWeapon(weapon)) {
                sender.sendMessage("§cHold a weapon in your main hand.");
                return true;
            }

            WeaponGrade grade;
            try {
                grade = WeaponGrade.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid grade. Use COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, or MYTHIC.");
                return true;
            }

            WeaponData wd = new WeaponData(weapon);
            wd.setGrade(grade);

            // Generate bonus stats according to grade
            java.util.List<com.chzzk.rpg.grade.BonusStat> newStats =
                com.chzzk.rpg.grade.BonusStatGenerator.generateForGrade(grade, wd.getBonusStatList());
            wd.setBonusStatList(newStats);
            wd.save();

            sender.sendMessage("§aWeapon grade set to " + grade.getColoredName());
            return true;
        }

        if (args[0].equalsIgnoreCase("weaponinfo")) {
            // /rpgadmin weaponinfo
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cPlayers only.");
                return true;
            }

            Player player = (Player) sender;
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (!WeaponData.isWeapon(weapon)) {
                sender.sendMessage("§cHold a weapon in your main hand.");
                return true;
            }

            WeaponData wd = new WeaponData(weapon);
            sender.sendMessage("§e=== 무기 정보 ===");
            sender.sendMessage("§7타입: §f" + wd.getWeaponType().name());
            sender.sendMessage("§7등급: " + wd.getGrade().getColoredName());
            sender.sendMessage("§7기본 공격력: §c" + wd.getBaseAtk());
            sender.sendMessage("§7강화 레벨: §a+" + wd.getEnhanceLevel());
            sender.sendMessage("§7총 공격력: §c" + wd.getTotalAtk());
            sender.sendMessage("§7공격 속도: §f" + wd.getAttackSpeed() + " APS");

            List<BonusStat> bonusStats = wd.getBonusStatList();
            if (!bonusStats.isEmpty()) {
                sender.sendMessage("§d보너스 스탯 (" + bonusStats.size() + "/" + wd.getGrade().getBonusStatSlots() + "):");
                for (int i = 0; i < bonusStats.size(); i++) {
                    sender.sendMessage("  §7" + (i + 1) + ". " + bonusStats.get(i).getDisplayString());
                }
            } else {
                sender.sendMessage("§7보너스 스탯: §8없음");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("givemoney")) {
            // /rpgadmin givemoney <player> <amount>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin givemoney <player> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            try {
                double amount = Double.parseDouble(args[2]);
                ChzzkRPG.getInstance().getVaultHook().deposit(target, amount);
                sender.sendMessage("§aGiven " + amount + " gold to " + target.getName());
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("setjob")) {
            // /rpgadmin setjob <player> <job>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin setjob <player> <NONE|BLACKSMITH|CHEF|BUILDER>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            LifeJob job;
            try {
                job = LifeJob.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid job. Use NONE, BLACKSMITH, CHEF, or BUILDER.");
                return true;
            }

            PlayerProfile profile = ChzzkRPG.getInstance().getStatsManager().getProfile(target);
            if (profile != null) {
                profile.setLifeJob(job);
                ChzzkRPG.getInstance().getStatsManager().saveProfile(target.getUniqueId());
                sender.sendMessage("§aSet " + target.getName() + "'s job to " + job.getDisplayName());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("setblacksmithlv")) {
            // /rpgadmin setblacksmithlv <player> <level>
            if (args.length < 3) {
                sender.sendMessage("Usage: /rpgadmin setblacksmithlv <player> <level>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            try {
                int level = Integer.parseInt(args[2]);
                PlayerProfile profile = ChzzkRPG.getInstance().getStatsManager().getProfile(target);
                if (profile != null) {
                    profile.setBlacksmithLevel(level);
                    ChzzkRPG.getInstance().getStatsManager().saveProfile(target.getUniqueId());
                    sender.sendMessage("§aSet " + target.getName() + "'s blacksmith level to " + level);
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("rerollstats")) {
            // /rpgadmin rerollstats
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cPlayers only.");
                return true;
            }

            Player player = (Player) sender;
            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (!WeaponData.isWeapon(weapon)) {
                sender.sendMessage("§cHold a weapon in your main hand.");
                return true;
            }

            WeaponData wd = new WeaponData(weapon);
            int statCount = wd.getGrade().getBonusStatSlots();

            if (statCount == 0) {
                sender.sendMessage("§c이 등급에는 보너스 스탯이 없습니다.");
                return true;
            }

            List<BonusStat> newStats = BonusStatGenerator.rerollAll(wd.getGrade(), statCount);
            wd.setBonusStatList(newStats);
            wd.save();

            sender.sendMessage("§a보너스 스탯이 재설정되었습니다!");
            for (int i = 0; i < newStats.size(); i++) {
                sender.sendMessage("  §7" + (i + 1) + ". " + newStats.get(i).getDisplayString());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("testkit")) {
            // /rpgadmin testkit [player]
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("Usage: /rpgadmin testkit <player>");
                return true;
            }

            // Give test items
            // 1. Weapon (EPIC grade)
            ItemStack weapon = ItemManager.createWeapon(Material.NETHERITE_SWORD, "§6테스트 무기", 50.0, target.getUniqueId());
            WeaponData wd = new WeaponData(weapon);
            wd.setGrade(WeaponGrade.EPIC);
            List<BonusStat> stats = BonusStatGenerator.generateForGrade(WeaponGrade.EPIC, wd.getBonusStatList());
            wd.setBonusStatList(stats);
            wd.save();
            target.getInventory().addItem(weapon);

            // 2. Grade stones
            target.getInventory().addItem(ItemManager.createGradeStone(20));

            // 3. Cubes
            target.getInventory().addItem(ItemManager.createCube(CubeType.BASIC, 10));
            target.getInventory().addItem(ItemManager.createCube(CubeType.ADVANCED, 5));

            // 4. Money
            ChzzkRPG.getInstance().getVaultHook().deposit(target, 1000000);

            // 5. Set as blacksmith
            PlayerProfile profile = ChzzkRPG.getInstance().getStatsManager().getProfile(target);
            if (profile != null) {
                profile.setLifeJob(LifeJob.BLACKSMITH);
                profile.setBlacksmithLevel(10);
                ChzzkRPG.getInstance().getStatsManager().saveProfile(target.getUniqueId());
            }

            sender.sendMessage("§a테스트 키트 지급 완료!");
            sender.sendMessage("§7- 영웅 등급 무기 (ATK 50)");
            sender.sendMessage("§7- 등급석 20개");
            sender.sendMessage("§7- 일반 큐브 10개, 고급 큐브 5개");
            sender.sendMessage("§7- 골드 1,000,000원");
            sender.sendMessage("§7- 대장장이 Lv.10 설정");
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }

        sender.sendMessage("§c알 수 없는 명령어입니다. /rpgadmin help");
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§e=== RPG Admin Commands ===");
        sender.sendMessage("§6/rpgadmin help §7- 도움말");
        sender.sendMessage("§6/rpgadmin reload §7- 설정 리로드");
        sender.sendMessage("");
        sender.sendMessage("§e[ 아이템 지급 ]");
        sender.sendMessage("§6/rpgadmin giveweapon <player> <atk> §7- 무기 지급");
        sender.sendMessage("§6/rpgadmin givescroll <player> <type> §7- 강화 주문서 지급");
        sender.sendMessage("§6/rpgadmin givegradestone <player> [amount] §7- 등급석 지급");
        sender.sendMessage("§6/rpgadmin givecube <player> <BASIC|ADVANCED> [amount] §7- 큐브 지급");
        sender.sendMessage("§6/rpgadmin givemoney <player> <amount> §7- 골드 지급");
        sender.sendMessage("");
        sender.sendMessage("§e[ 무기 수정 (손에 들고) ]");
        sender.sendMessage("§6/rpgadmin setgrade <등급> §7- 무기 등급 설정");
        sender.sendMessage("§6/rpgadmin rerollstats §7- 보너스 스탯 재설정");
        sender.sendMessage("§6/rpgadmin weaponinfo §7- 무기 정보 확인");
        sender.sendMessage("");
        sender.sendMessage("§e[ 플레이어 설정 ]");
        sender.sendMessage("§6/rpgadmin setpoints <player> <amount> §7- 스탯 포인트 설정");
        sender.sendMessage("§6/rpgadmin setjob <player> <job> §7- 생활 직업 설정");
        sender.sendMessage("§6/rpgadmin setblacksmithlv <player> <lv> §7- 대장장이 레벨 설정");
        sender.sendMessage("");
        sender.sendMessage("§e[ 테스트 ]");
        sender.sendMessage("§6/rpgadmin testkit [player] §7- 테스트 키트 지급");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("rpg.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "giveweapon":
            case "givescroll":
            case "givegradestone":
            case "givemoney":
            case "setpoints":
            case "setblacksmithlv":
            case "testkit":
                if (args.length == 2) {
                    return getOnlinePlayerNames(args[1]);
                }
                break;

            case "givecube":
                if (args.length == 2) {
                    return getOnlinePlayerNames(args[1]);
                }
                if (args.length == 3) {
                    return filterStartsWith(CUBE_TYPES, args[2]);
                }
                break;

            case "setgrade":
                if (args.length == 2) {
                    return filterStartsWith(GRADES, args[1]);
                }
                break;

            case "setjob":
                if (args.length == 2) {
                    return getOnlinePlayerNames(args[1]);
                }
                if (args.length == 3) {
                    return filterStartsWith(JOBS, args[2]);
                }
                break;
        }

        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
}
