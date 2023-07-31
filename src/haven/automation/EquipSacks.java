package haven.automation;


import haven.AltBeltWnd;
import haven.Config;
import haven.Coord;
import haven.Equipory;
import haven.GItem;
import haven.GameUI;
import haven.Inventory;
import haven.InventoryBelt;
import haven.WItem;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class EquipSacks implements Runnable {
    private GameUI gui;
    private static final int TIMEOUT = 2000;
    private static final Coord sqsz = new Coord(36, 33);

    public EquipSacks(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        try {//start giant try catch to prevent any loading/null crashes when using this.
            Inventory beltInv = null;
            InventoryBelt quickBeltInv = null;
            WItem righthand = gui.getequipory().quickslots[7];
            WItem lefthand = gui.getequipory().quickslots[6];
            WItem belteq = gui.getequipory().quickslots[5];
            HashMap<WItem, Integer> wepmap = new HashMap<>();
            int iterations = 0;


            if (righthand != null && lefthand != null) {
                if (righthand.name.get().contains("Sack") && lefthand.name.get().contains("Sack")) {
                    PBotUtils.debugMsg(gui.ui, "Already found traveler sacks, canceling.", Color.white);
                    return;
                }
                if (righthand.name.get().contains("Bindle") && lefthand.name.get().contains("Bindle")) {
                    PBotUtils.debugMsg(gui.ui, "Already found traveler sacks, canceling.", Color.white);
                    return;
                }
            }
            if (righthand == null && lefthand == null) {//if hands are empty obviously we need to run this twice to attempt to equip 2 sacks.
                iterations = 2;
            } else { //else figure out if we already have 1 sack equipped when we run
                if (righthand != null) {
                    if (!righthand.name.get().contains("Sack") && !righthand.name.get().contains("Bindle"))
                        iterations++;
                } else
                    iterations++;
                if (lefthand != null) {
                    if (!lefthand.name.get().contains("Sack") && !lefthand.name.get().contains("Bindle"))
                        iterations++;
                } else
                    iterations++;
            }

            // System.out.println("equip sack iterations : "+iterations);

            for (int p = 0; p < iterations; p++) {
                wepmap.putAll(getWeapon(gui.maininv));
                if (Config.quickbelt) {
                    Widget belt = null;
                    for (Widget w = gui.lchild; w != null; w = w.prev) {
                        if (w instanceof AltBeltWnd) {
                            belt = w;
                            break;
                        }
                    }

                    if (belt != null) {
                        for (Widget w = belt.lchild; w != null; w = w.prev) {
                            if (w instanceof InventoryBelt) {
                                wepmap.putAll(getWeaponQuickBelt((InventoryBelt) w));
                                quickBeltInv = (InventoryBelt) w;
                                break;
                            }
                        }
                        if (quickBeltInv == null) return;
                    } else {
                        if (belteq != null && belteq.item.contents instanceof Inventory) {
                            wepmap.putAll(getWeapon((Inventory) belteq.item.contents));
                            beltInv = (Inventory) belteq.item.contents;
                        }
                        if (beltInv == null) return;
                    }
                } else {
                    Window belt = gui.getwnd("Belt");
                    if (belt != null) {
                        for (Widget w = belt.lchild; w != null; w = w.prev) {
                            if (w instanceof Inventory) {
                                wepmap.putAll(getWeapon((Inventory) w));
                                beltInv = (Inventory) w;
                                break;
                            }
                        }
                        if (beltInv == null) return;
                    } else {
                        if (belteq != null && belteq.item.contents instanceof Inventory) {
                            wepmap.putAll(getWeapon((Inventory) belteq.item.contents));
                            beltInv = (Inventory) belteq.item.contents;
                        }
                        if (beltInv == null) return;
                    }
                }


                if (wepmap.size() == 0) {
                    PBotUtils.debugMsg(gui.ui, "No sacks found", Color.white);
                    return;
                }

                GItem weaponItem;

                List<WItem> weapons = new ArrayList<>(wepmap.keySet());
                Collections.sort(weapons, new Comparator<WItem>() {
                    @Override
                    public int compare(WItem s1, WItem s2) {
                        Integer popularity1 = wepmap.get(s1);
                        Integer popularity2 = wepmap.get(s2);
                        return popularity1.compareTo(popularity2);
                    }
                });
                weaponItem = weapons.get(weapons.size() - 1).item;

                weaponItem.wdgmsg("take", new Coord(weaponItem.sz.x / 2, weaponItem.sz.y / 2));

                try {
                    if (!Utils.waitForOccupiedHand(gui, TIMEOUT, "waitForOccupiedHand timed-out"))
                        return;
                } catch (InterruptedException ie) {
                    return;
                }

                Equipory e = gui.getequipory();
                if (e == null)//equipory is somehow null, break
                    return;

                if (righthand == null) //try to find an empty hand first, otherwise drop it in left hand
                    e.wdgmsg("drop", 7);
                else if (lefthand == null)
                    e.wdgmsg("drop", 6);
                else {//resolve what's in both hands to ensure we don't overwrite a sack with another sack
                    if (!righthand.name.get().contains("Sack") && !righthand.name.get().contains("Bindle"))
                        e.wdgmsg("drop", 7);
                    else if (!lefthand.name.get().contains("Sack") && !lefthand.name.get().contains("Bindle"))
                        e.wdgmsg("drop", 6);
                    try {
                        if (!Utils.waitForOccupiedHand(gui, TIMEOUT, "waitForOccupiedHand2 timed-out"))
                            return;
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
                PBotUtils.sleep(250);


                GItem hand = PBotUtils.getGItemAtHand(gui.ui);
                if (hand != null) { //try to empty hand into belt
                    if (beltInv != null) {
                        List<Coord> slots = PBotUtils.getFreeInvSlots(beltInv);
                        for (Coord i : slots) {
                            if (PBotUtils.getGItemAtHand(gui.ui) == null)
                                break;
                            PBotUtils.dropItemToInventory(i, beltInv);
                            PBotUtils.sleep(100);
                        }
                    } else if (quickBeltInv != null) {
                        List<Coord> slots = quickBeltInv.getFreeSlots();
                        for (Coord i : slots) {
                            if (PBotUtils.getGItemAtHand(gui.ui) == null)
                                break;
                            Coord dc = i.add(sqsz.div(2)).div(sqsz);
                            // convert single row coordinate into multi-row
                            if (dc.x >= quickBeltInv.isz.x) {
                                dc.y = dc.x / quickBeltInv.isz.x;
                                dc.x = dc.x % quickBeltInv.isz.x;
                            }
                            quickBeltInv.wdgmsg("drop", dc);
                            PBotUtils.sleep(100);
                        }
                    }
                }
                if (gui.vhand != null) { //hand still not empty, dump into main inventory
                    List<Coord> slots = PBotUtils.getFreeInvSlots(gui.maininv);
                    for (Coord i : slots) {
                        if (PBotUtils.getGItemAtHand(gui.ui) == null)
                            break;
                        PBotUtils.dropItemToInventory(i, gui.maininv);
                        PBotUtils.sleep(100);
                    }
                }
                beltInv = null;
                quickBeltInv = null;
                righthand = gui.getequipory().quickslots[7];
                lefthand = gui.getequipory().quickslots[6];
                wepmap.clear();
            }
        } catch (Exception e) {
            PBotUtils.debugMsg(gui.ui, "Exception occurred in EquipSack script, ignored.", Color.white);
            e.printStackTrace();
        }//ignore all exceptions, this script will likely be used in a combat situation and crashes are unacceptable
    }

    private HashMap<WItem, Integer> getWeapon(Inventory inv) {
        HashMap<WItem, Integer> map = new HashMap<>();
        int priority;
        WItem weapon = inv.getItemPartial("Sack");
        priority = 3;
        if (weapon == null) {
            weapon = inv.getItemPartial("Bindle");
            priority = 2;
        }
        if (weapon != null)
            map.put(weapon, priority);
        return map;
    }

    private HashMap<WItem, Integer> getWeaponQuickBelt(InventoryBelt inv) {
        HashMap<WItem, Integer> map = new HashMap<>();
        int priority = 0;
        WItem weapon = inv.getItemPartial("Sack");
        priority = 3;
        if (weapon == null) {
            weapon = inv.getItemPartial("Bindle");
            priority = 2;
        }
        if (weapon != null)
            map.put(weapon, priority);
        return map;
    }
}
