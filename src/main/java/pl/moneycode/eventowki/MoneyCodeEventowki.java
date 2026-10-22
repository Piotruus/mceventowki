package pl.moneycode.eventowki;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

public final class MoneyCodeEventowki extends JavaPlugin implements Listener {
  private NamespacedKey key;
  private static final String TITLE = "§5§l✦ §dMoneyCode §8• §fEventówki";
  private static final String[] IDS = {"axe","hook","sword","pear","pickaxe","bomb","hourglass"};
  private static final Material[] BASE = {Material.NETHERITE_AXE,Material.CARROT_ON_A_STICK,Material.NETHERITE_SWORD,Material.DRIED_KELP,Material.NETHERITE_PICKAXE,Material.FIRE_CHARGE,Material.CLOCK};
  private static final String[] NAME = {"§b§l⚡ Topór Gromu","§5§l⛓ Hak Cieni","§c§l🩸 Miecz Krwi","§e§l🍐 Gruszka","§3§l⛏ Kilof Przeklęty","§c§l✹ Bomba","§d§l⌛ Klepsydra Czasu"};
  private static final String[] EVENT = {"§b❄ ZIMOWEGO","§5☾ HALLOWEENOWEGO","§5☠ HALLOWEENOWEGO","§6✦ JESIENNEGO","§3❄ ZIMOWEGO","§c✹ NOWOROCZNEGO","§d✧ MAGICZNEGO"};
  private static final int[] CD = {50,45,60,0,55,60,80};
  private final Map<UUID,Map<String,Long>> cooldowns = new HashMap<>();
  private final Map<UUID,String> projectiles = new HashMap<>();

  @Override public void onEnable() {
    saveDefaultConfig();
    key = new NamespacedKey(this,"moneycode_item");
    Bukkit.getPluginManager().registerEvents(this,this);
    Objects.requireNonNull(getCommand("mceventowki")).setExecutor((sender,cmd,label,args)->{
      if(!(sender instanceof Player p)){sender.sendMessage("Komenda tylko dla graczy.");return true;}
      if(!p.isOp()&&!p.hasPermission("moneycode.eventowki.admin")){p.sendMessage("§cBrak uprawnień.");return true;}
      if(args.length>0 && args[0].equalsIgnoreCase("reload")){reloadConfig();p.sendMessage("§aPrzeładowano konfigurację.");return true;}
      Inventory inv=Bukkit.createInventory(null,27,TITLE);
      int[] slots={10,11,12,13,14,15,16};
      for(int i=0;i<IDS.length;i++)inv.setItem(slots[i],item(i));
      p.openInventory(inv);return true;
    });
    getLogger().info("MoneyCode Eventowki 2.1 - loaded on " + Bukkit.getBukkitVersion());
  }
  private String[] details(int i){return switch(i){
    case 0 -> new String[]{"§7Wywołuje falę piorunów w promieniu §b5 bloków§7.","§7Zadaje §c3 serca §7i spowalnia na §b3 sekundy§7."};
    case 1 -> new String[]{"§7Wystrzeliwuje hak w przeciwnika.","§7Przyciąga trafionego gracza z §d12 bloków§7."};
    case 2 -> new String[]{"§7Trafienia mieczem przywracają §c1 serce§7.","§7PPM: §cSiła I §7na 5 sekund."};
    case 3 -> new String[]{"§7Rzadki owoc, którego nie da się zdobyć", "§7w zwykłym survivalu. §dRegeneracja V §7na §f3 s§7.","§7Dodatkowo: §cSiła I §7na §f15 s§7."};
    case 4 -> new String[]{"§7PPM: §bSzybkość II §7na 8 sekund.","§7Kopie szybciej, ale §cnie niszczy bloków mocą§7."};
    case 5 -> new String[]{"§7PPM: rzuć bombę, która wybucha bez", "§7niszczenia terenu. §c2 serca §7w promieniu §f4 bloków§7."};
    default -> new String[]{"§7PPM: spowalnia wrogów w promieniu §d6 bloków§7.","§7Daje Tobie §bSzybkość II §7na 5 sekund."};
  };}
  private ItemStack item(int i){
    ItemStack stack=new ItemStack(BASE[i]);ItemMeta meta=stack.getItemMeta();
    meta.setDisplayName(NAME[i]);meta.setCustomModelData(12001+i);
    meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,IDS[i]);
    List<String> lore=new ArrayList<>();
    lore.add("§8§m━━━━━━━━━━━━━━━━━━━━━━━━");
    lore.add("§7✦ §fPrzedmiot z §dEVENTU " + EVENT[i]);
    lore.add("§8 ");lore.add("§d§l✧ UMIEJĘTNOŚĆ");
    lore.addAll(Arrays.asList(details(i)));lore.add("§8  ");
    lore.add(i==3?"§e➤ §fZjedz, aby użyć":"§e➤ §fKliknij §ePPM§f, aby użyć");
    lore.add(i==3?"§7✦ Przedmiot jednorazowy":"§7⌛ Odnowienie: §f"+CD[i]+" sekund");
    lore.add("§8§m━━━━━━━━━━━━━━━━━━━━━━━━");
    meta.setLore(lore);stack.setItemMeta(meta);return stack;
  }
  private String id(ItemStack s){if(s==null||!s.hasItemMeta())return null;return s.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);}
  @EventHandler public void click(InventoryClickEvent e){
    if(!TITLE.equals(e.getView().getTitle()))return;
    e.setCancelled(true);
    if(e.getClickedInventory()!=e.getView().getTopInventory()||!(e.getWhoClicked() instanceof Player p))return;
    if(!p.isOp()&&!p.hasPermission("moneycode.eventowki.admin"))return;
    String id=id(e.getCurrentItem());if(id==null)return;
    for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id)){
      if(!p.getInventory().addItem(item(i)).isEmpty())p.sendMessage("§cEkwipunek jest pełny.");
      else p.sendMessage("§aOtrzymano §f"+NAME[i]);return;
    }
  }
  private boolean allowed(Location loc){
    if(loc.getWorld()==null)return false;
    if(Bukkit.getPluginManager().getPlugin("WorldGuard")==null)return true;
    var mgr=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
    if(mgr==null)return getConfig().getBoolean("allow-without-region",true);
    boolean has=false,pvp=false;
    String block=getConfig().getString("blocked-region-keyword","spawn").toLowerCase(Locale.ROOT);
    String allow=getConfig().getString("allowed-region-keyword","pvp").toLowerCase(Locale.ROOT);
    for(ProtectedRegion r:mgr.getApplicableRegions(BlockVector3.at(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ()))){
      String n=r.getId().toLowerCase(Locale.ROOT);has=true;
      if(n.contains(block))return false;
      if(n.contains(allow))pvp=true;
    }
    return pvp||(!has&&(getConfig().getBoolean("allow-without-region",true)||getConfig().getStringList("worlds-always-allow-without-region").contains(loc.getWorld().getName())));
  }
  private boolean ready(Player p,String type,int sec){
    Map<String,Long> map=cooldowns.computeIfAbsent(p.getUniqueId(),k->new HashMap<>());
    long now=System.currentTimeMillis(),until=map.getOrDefault(type,0L);
    if(now<until){p.sendMessage("§c⌛ Poczekaj jeszcze §f"+((until-now+999)/1000)+" s§c.");return false;}
    map.put(type,now+sec*1000L);return true;
  }
  @EventHandler(priority=EventPriority.HIGH) public void use(PlayerInteractEvent e){
    if(e.getHand()!=EquipmentSlot.HAND||(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK))return;
    Player p=e.getPlayer();String type=id(e.getItem());if(type==null||type.equals("pear"))return;
    e.setCancelled(true);
    if(!allowed(p.getLocation())){p.sendMessage("§c✘ Moc jest zablokowana na tym regionie.");return;}
    int i=Arrays.asList(IDS).indexOf(type);if(i<0||!ready(p,type,CD[i]))return;
    switch(type){
      case "axe" -> {
        p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,p.getLocation().add(0,1,0),90,3,1,3,0.12);
        for(Entity entity:p.getNearbyEntities(5,5,5))if(entity instanceof Player v&&!v.equals(p)&&allowed(v.getLocation())&&p.hasLineOfSight(v)){
          v.damage(6,p);v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,1));
        }
        p.sendMessage("§b⚡ Uwolniono Grom!");
      }
      case "hook","bomb" -> {
        Snowball ball=p.launchProjectile(Snowball.class);ball.setVelocity(p.getEyeLocation().getDirection().multiply(type.equals("hook")?2.0:1.5));
        projectiles.put(ball.getUniqueId(),type);p.sendMessage(type.equals("bomb")?"§c✹ Bomba wystrzelona!":"§d⛓ Hak wystrzelony!");
      }
      case "sword" -> {p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,100,0));p.sendMessage("§c🩸 Miecz Krwi: Siła I (5 s)!");}
      case "pickaxe" -> {p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,160,1));p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,160,1));p.sendMessage("§3⛏ Przeklęta szybkość aktywna!");}
      case "hourglass" -> {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,100,1));
        for(Entity entity:p.getNearbyEntities(6,6,6))if(entity instanceof Player v&&!v.equals(p)&&allowed(v.getLocation())&&p.hasLineOfSight(v))v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,100,2));
        p.getWorld().spawnParticle(Particle.ENCHANT,p.getLocation().add(0,1,0),70,3,1,3,0.2);
        p.sendMessage("§d⌛ Czas zwolnił!");
      }
    }
  }
  @EventHandler public void swordHit(org.bukkit.event.entity.EntityDamageByEntityEvent e){
    if(!(e.getDamager() instanceof Player p)||!"sword".equals(id(p.getInventory().getItemInMainHand()))||!(e.getEntity() instanceof Player v))return;
    if(!allowed(p.getLocation())||!allowed(v.getLocation()))return;
    p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+2.0));
    p.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,v.getLocation().add(0,1,0),8,0.3,0.4,0.3,0);
  }
  @EventHandler public void consume(PlayerItemConsumeEvent e){
    if(!"pear".equals(id(e.getItem())))return;
    Player p=e.getPlayer();if(!allowed(p.getLocation())){e.setCancelled(true);p.sendMessage("§cGruszka nie działa na spawnie.");return;}
    Bukkit.getScheduler().runTask(this,()->{
      if(!p.isOnline())return;
      p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,60,4));
      p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,300,0));
      p.sendMessage("§e🍐 Gruszka: §dRegeneracja V (3 s) §7+ §cSiła I (15 s)");
    });
  }
  @EventHandler public void hit(org.bukkit.event.entity.ProjectileHitEvent e){
    String type=projectiles.remove(e.getEntity().getUniqueId());if(type==null)return;
    if(!(e.getEntity().getShooter() instanceof Player p))return;
    Location at=e.getHitEntity()!=null?e.getHitEntity().getLocation():e.getEntity().getLocation();
    if(!allowed(at)||!allowed(p.getLocation())){p.sendMessage("§cCel znajduje się w chronionym regionie.");return;}
    if(type.equals("hook")){
      if(e.getHitEntity() instanceof Player v&&!v.equals(p)&&v.getLocation().distanceSquared(p.getLocation())<=144){
        Vector pull=p.getLocation().toVector().subtract(v.getLocation().toVector()).normalize().multiply(1.3).setY(0.35);
        v.setVelocity(pull);p.sendMessage("§d⛓ Przyciągnięto przeciwnika!");
      }
    }else{
      World w=at.getWorld();if(w==null)return;
      w.spawnParticle(Particle.EXPLOSION,at,2,0.2,0.2,0.2,0);
      w.playSound(at,Sound.ENTITY_GENERIC_EXPLODE,0.8f,1.2f);
      for(Entity entity:w.getNearbyEntities(at,4,4,4))if(entity instanceof Player v&&!v.equals(p)&&allowed(v.getLocation())){
        v.damage(4,p);Vector away=v.getLocation().toVector().subtract(at.toVector());if(away.lengthSquared()>0.01)v.setVelocity(away.normalize().multiply(0.65).setY(0.3));
      }
      p.sendMessage("§c✹ Bomba wybuchła bez niszczenia bloków!");
    }
    e.getEntity().remove();
  }
}
