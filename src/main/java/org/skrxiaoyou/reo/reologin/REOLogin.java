package org.skrxiaoyou.reo.reologin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;

public final class REOLogin extends JavaPlugin implements Listener {

    private HashMap<UUID, UserData> user_data; //存储玩家信息

    public static String getMD5(String s) { //将玩家密码进行MD5加密
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(s.getBytes());
            byte[] md5 = digest.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : md5) {
                if (b < 0)
                    b += 256;
                result.append(Integer.toHexString(b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("MD5加密错误!");
        }
    }

    public void loadUserData() { //读取玩家信息
        String path = getDataFolder() + File.separator + "user_data.dat";
        if (new File(path).exists()) { //判断存储文件是否被创建
            try {
                FileInputStream fis = new FileInputStream(path);
                ObjectInputStream ois = new ObjectInputStream(fis);
                user_data = (HashMap<UUID, UserData>) ois.readObject();
                ois.close();
                fis.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveUserData() { //写入玩家信息
        String path = getDataFolder() + File.separator + "user_data.dat";
        try {
            getDataFolder().mkdirs(); //创建目录
            FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(user_data);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) { //在玩家进入服务器时发送信息
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!user_data.containsKey(uuid)) { //判断玩家是否注册过
            player.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "若要在此服务器中游玩,请先注册!");
            player.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.WHITE + "使用 " + ChatColor.AQUA + "\"/register <password>\"" + ChatColor.WHITE + " 进行注册.");
        } else {
            UserData userdata = user_data.get(uuid);
            if (System.currentTimeMillis() - userdata.last_login_timestamp <= 0) { //判断玩家是否是在离线保留时长内重新登录
                player.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.WHITE + "欢迎游玩REO三服!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "请先登录!");
                player.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.WHITE + "使用 " + ChatColor.AQUA + "\"/login <password>" + ChatColor.WHITE + " 进行登录.");
            }
        }
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) { //在玩家破坏方块时
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!user_data.containsKey(uuid)) {
            event.setCancelled(true); //取消未登录玩家破坏方块的行为
        } else {
            UserData userdata = user_data.get(uuid);
            if (System.currentTimeMillis() - userdata.last_login_timestamp > 0) { //判断玩家是否是在离线保留时长外登录
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) { //在玩家移动时
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!user_data.containsKey(uuid)) {
            event.setCancelled(true); //取消未登录玩家移动的行为
        } else {
            UserData userdata = user_data.get(uuid);
            if (System.currentTimeMillis() - userdata.last_login_timestamp > 0) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { //在命令输入时
        if (command.getName().equalsIgnoreCase("register")) { //判断玩家使用的是否是注册命令
            if (!(sender instanceof Player)) { //判断输入注册命令的是否是玩家
                sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "此命令只能由玩家输入!");
            } else {
                if (user_data.containsKey(((Player) sender).getUniqueId())) //判断玩家是否注册过
                    sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "你已经注册过了!");
                else {
                    if (args.length > 1) { //判断输入信息是否合法
                        return false;
                    } else {
                        UserData userdata = new UserData(getMD5(args[0]), 0);
                        user_data.put(((Player) sender).getUniqueId(), userdata);
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.WHITE + "注册成功,现在你可以使用 " + ChatColor.AQUA + "\"/login <password>\"" + ChatColor.WHITE + " 进行登录.");
                    }
                }
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("login")) { //判断玩家使用的是否是登录命令
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "此命令只能由玩家输入!");
            } else {
                if (args.length > 1) { //判断输入信息是否合法
                    return false;
                } else {
                    if (!user_data.containsKey(((Player) sender).getUniqueId())) { //判断该玩家是否注册过(没有注册过就使用登录命令)
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "你还没有注册过,请先注册!");
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " +ChatColor.WHITE +  "使用 " + ChatColor.AQUA + "\"/register <password>\"" + ChatColor.WHITE + " 进行注册.");
                    } else if (System.currentTimeMillis() - user_data.get(((Player) sender).getUniqueId()).last_login_timestamp <= 0) { //判断该玩家是否已经登录
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "你已经成功登录了!");
                    } else if (user_data.get(((Player) sender).getUniqueId()).encrypted_password.equals(getMD5(args[0]))) { //密码判断
                        UserData userdata = new UserData(getMD5(args[0]), System.currentTimeMillis());
                        user_data.put(((Player) sender).getUniqueId(), userdata);
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.AQUA + "登录成功!");
                    } else { //在上个if失败的情况下(输入密码错误的情况下)
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "密码错误!");
                    }
                }
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("changepwd")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "此命令只能由玩家输入!");
            } else {
                if (args.length > 1) {
                    return false;
                } else {
                    if (!user_data.containsKey(((Player) sender).getUniqueId())) {
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.RED + "你还没有注册过,请先注册!");
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.WHITE + "使用 " + ChatColor.AQUA + "\"/register <password>\"" + ChatColor.WHITE + " 进行注册.");
                    } else if (System.currentTimeMillis() - user_data.get(((Player) sender).getUniqueId()).last_login_timestamp <= 0) {
                        UserData userdata = new UserData(getMD5(args[0]), 0);
                        user_data.put(((Player) sender).getUniqueId(), userdata);
                        sender.sendMessage(ChatColor.YELLOW + "[REO登录插件] " + ChatColor.WHITE + "更换密码成功,下次登录请使用新密码.");
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onEnable() { //插件启用时
        user_data = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        loadUserData(); //导入玩家信息
    }

    @Override
    public void onDisable() { //插件禁用时
        saveUserData(); //存储玩家信息
    }
}
