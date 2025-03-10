Haven Resource 1( src   Landwindow.java /* Preprocessed source code */
/* -*- Java -*- */
/* $use: ui/apower */

import haven.*;
import haven.res.ui.apower.*;
import java.awt.image.BufferedImage;
import java.awt.Color;

/*
 * >wdg: Landwindow
 */
public class Landwindow extends Window {
    Widget bn, be, bs, bw, refill, charge, buy, reset, dst, rebond;
    BuddyWnd.GroupSelector group;
    Label area, cost;
    Widget authmeter;
    PowerMeter apower;
    int auth, acap, adrain;
    boolean offline;
    Coord c1, c2, cc1, cc2;
    MCache.Overlay ol;
    MCache map;
    int bflags[] = new int[8];
    PermBox perms[] = new PermBox[4];
    CheckBox homeck;
    private static final String fmt = "Area: %d m" + ((char)0xB2);

    public static Widget mkwidget(UI ui, Object... args) {
	Coord c1 = (Coord)args[0];
	Coord c2 = (Coord)args[1];
	return(new Landwindow(c1, c2));
    }
	
    private void fmtarea() {
	area.settext(String.format(fmt, (c2.x - c1.x + 1) * (c2.y - c1.y + 1)));
    }

    private void updatecost() {
	cost.settext(String.format("Cost: %d", 10 * (((cc2.x - cc1.x + 1) * (cc2.y - cc1.y + 1)) - ((c2.x - c1.x + 1) * (c2.y - c1.y + 1)))));
    }

    private void updflags() {
	int fl = bflags[group.group];
	for(PermBox w : perms)
	    w.a = (fl & w.fl) != 0;
    }

    private class PermBox extends CheckBox {
	int fl;
	
	PermBox(String lbl, int fl) {
	    super(lbl);
	    this.fl = fl;
	}
	
	public void changed(boolean val) {
	    int fl = 0;
	    for(PermBox w : perms) {
		if(w.a)
		    fl |= w.fl;
	    }
	    Landwindow.this.wdgmsg("shared", group.group, fl);
	    bflags[group.group] = fl;
	}
    }

    private Tex rauth = null;

    public Landwindow(Coord c1, Coord c2) {
	super(new Coord(0, 0), "Stake", false);
	this.cc1 = this.c1 = c1;
	this.cc2 = this.c2 = c2;
	int y = 0;
	area = add(new Label(""), new Coord(0, y)); y += 15;
	authmeter = add(new Widget(new Coord(300, 20)) {
		public void draw(GOut g) {
		    int auth = Landwindow.this.auth;
		    int acap = Landwindow.this.acap;
		    if(acap > 0) {
			g.chcolor(0, 0, 0, 255);
			g.frect(Coord.z, sz);
			g.chcolor(128, 0, 0, 255);
			Coord isz = sz.sub(2, 2);
			isz.x = (auth * isz.x) / acap;
			g.frect(new Coord(1, 1), isz);
			g.chcolor();
			if(rauth == null) {
			    Color col = offline?Color.RED:Color.WHITE;
			    rauth = new TexI(Utils.outline2(Text.render(String.format("%s/%s", auth, acap), col).img, Utils.contrast(col)));
			}
			g.aimage(rauth, sz.div(2), 0.5, 0.5);
		    }
		}
	    }, new Coord(0, y)); y += 25;
	apower = add(new PowerMeter(new Coord(300, 20), 5), new Coord(0, y)); y += 25;
	refill = add(new Button(140, "Refill"), new Coord(0, y));
	refill.tooltip = RichText.render("Refill this claim's presence immediately from your current pool of learning points.", 300);
	charge = add(new Button(140, "Charge"), new Coord(160, y));
	y += 40;
	cost = add(new Label("Cost: 0"), new Coord(0, y)); y += 25;
	fmtarea();
	bn = add(new Button(120, "Extend North"),   90,   y);
	be = add(new Button(120, "Extend East"),    180,  y + 25);
	bs = add(new Button(120, "Extend South"),   90,   y + 50);
	bw = add(new Button(120, "Extend West"),    0,    y + 25);
	y += 100;
	buy = add(new Button(140, "Buy"), 0, y);
	reset = add(new Button(140, "Reset"), 160, y);
	dst = add(new Button(140, "Declaim"), 0, y + 35);
	rebond = add(new Button(140, "Renew bond"), 160, y + 35);
	rebond.tooltip = RichText.render("Create a new bond for this claim, destroying the old one. Costs half of this claim's total presence.", 300);
	y += 80;
	add(new Label("Assign permissions to memorized people:"), 0, y); y += 15;
	group = add(new BuddyWnd.GroupSelector(0) {
		protected void changed(int g) {
		    super.changed(g);
		    updflags();
		}
	    }, 0, y);
	y += 30;
	perms[0] = add(new PermBox("Trespassing", 1), 10, y); y += 20;
	perms[3] = add(new PermBox("Rummaging", 8), 10, y); y += 20;
	perms[1] = add(new PermBox("Theft", 2), 10, y); y += 20;
	perms[2] = add(new PermBox("Vandalism", 4), 10, y); y += 20;
	add(new Label("White permissions also apply to non-memorized people."), 0, y); y += 15;
	pack();
    }

    protected void added() {
	super.added();
	map = ui.sess.glob.map;
	getparent(GameUI.class).map.enol(0, 1, 16);
	ol = map.new Overlay(cc1, cc2, 65536);
    }

    public void destroy() {
	getparent(GameUI.class).map.disol(0, 1, 16);
	ol.destroy();
	super.destroy();
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "upd") {
	    Coord c1 = (Coord)args[0];
	    Coord c2 = (Coord)args[1];
	    this.c1 = c1;
	    this.c2 = c2;
	    fmtarea();
	    updatecost();
	} else if(msg == "shared") {
	    int g = (Integer)args[0];
	    int fl = (Integer)args[1];
	    bflags[g] = fl;
	    if(g == group.group)
		updflags();
	} else if(msg == "auth") {
	    auth = (Integer)args[0];
	    acap = (Integer)args[1];
	    adrain = (Integer)args[2];
	    offline = (Integer)args[3] != 0;
	    rauth = null;
	} else if(msg == "apow") {
	    apower.set(((Number)args[0]).doubleValue());
	} else if(msg == "entime") {
	    int entime = (Integer)args[0];
	    authmeter.tooltip = Text.render(String.format("%d:%02d until enabled", entime / 3600, (entime % 3600) / 60));
	}
    }
	
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == bn) {
	    cc1 = cc1.add(0, -1);
	    ol.update(cc1, cc2);
	    updatecost();
	    return;
	} else if(sender == be) {
	    cc2 = cc2.add(1, 0);
	    ol.update(cc1, cc2);
	    updatecost();
	    return;
	} else if(sender == bs) {
	    cc2 = cc2.add(0, 1);
	    ol.update(cc1, cc2);
	    updatecost();
	    return;
	} else if(sender == bw) {
	    cc1 = cc1.add(-1, 0);
	    ol.update(cc1, cc2);
	    updatecost();
	    return;
	} else if(sender == buy) {
	    wdgmsg("take", cc1, cc2);
	    return;
	} else if(sender == reset) {
	    ol.update(cc1 = c1, cc2 = c2);
	    updatecost();
	    return;
	} else if(sender == dst) {
	    wdgmsg("declaim");
	    return;
	} else if(sender == rebond) {
	    wdgmsg("bond");
	    return;
	} else if(sender == refill) {
	    wdgmsg("refill");
	    return;
	} else if(sender == charge) {
	    wdgmsg("charge");
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }
}
code B  Landwindow$2 ����   4 #	  
  
  
     this$0 LLandwindow; <init> (LLandwindow;I)V Code LineNumberTable changed (I)V 
SourceFile Landwindow.java EnclosingMethod  	    	       Landwindow$2 InnerClasses ! haven/BuddyWnd$GroupSelector GroupSelector 
Landwindow (Lhaven/Coord;Lhaven/Coord;)V 
access$100 (LLandwindow;)V haven/BuddyWnd 
land.cjava               	 
     #     *+� *� �           r        -     *� *� � �           t  u  v      "                	      code   Landwindow ����   4�
  �	  � � �
  �	  � � �	  �	  �	  �	  �
 e �
 � �
  �	  � �	  �	  �	  �	  �	 H �	  �	  �	  �
  � �
 | � � � �
  �
  � �
 " �	  � �
 % �	  � � �
 ( �	  � �
 � �	 � � �	  � �
  
 	 	 	 	 	
	 	 	 	 
 F
 
 
 |	  	!"	#$	%&	 &'
 (	 V)
*+-
 .   
 Z/	 0
*1
 Z2
 |23
 456
 e7 �	 8	 9	 :	 ;<=
 m>
 %?@A
BC
 D
 ZEF
 GHI � �
 |JK PermBox InnerClasses bn Lhaven/Widget; be bs bw refill charge buy reset dst rebond group GroupSelector Lhaven/BuddyWnd$GroupSelector; area Lhaven/Label; cost 	authmeter apower  Lhaven/res/ui/apower/PowerMeter; auth I acap adrain offline Z c1 Lhaven/Coord; c2 cc1 cc2 ol Overlay Lhaven/MCache$Overlay; map Lhaven/MCache; bflags [I perms [LLandwindow$PermBox; homeck Lhaven/CheckBox; fmt Ljava/lang/String; ConstantValue rauth Lhaven/Tex; mkwidget -(Lhaven/UI;[Ljava/lang/Object;)Lhaven/Widget; Code LineNumberTable fmtarea ()V 
updatecost updflags StackMapTable � � � <init> (Lhaven/Coord;Lhaven/Coord;)V added destroy uimsg ((Ljava/lang/String;[Ljava/lang/Object;)VLM wdgmsg 6(Lhaven/Widget;Ljava/lang/String;[Ljava/lang/Object;)V 
access$000 (LLandwindow;)Lhaven/Tex; 
access$002 $(LLandwindow;Lhaven/Tex;)Lhaven/Tex; 
access$100 (LLandwindow;)V 
SourceFile Landwindow.java � � � � haven/Coord 
Landwindow � � � � Area: %d m² java/lang/Object � �N � � �O �PQLRSTU � � Cost: %d � � � � � � � � � � � �V �W � �X Stake �Y Landwindow$PermBox haven/Label   �UZ[ Landwindow$1 �\ � � haven/res/ui/apower/PowerMeter �] � � haven/Button Refill �^ � � SRefill this claim's presence immediately from your current pool of learning points._`abcd Charge � � Cost: 0 � � Extend NorthZe  � Extend East � � Extend South � � Extend West � � Buy � � Reset � � Declaim � � 
Renew bond � � dCreate a new bond for this claim, destroying the old one. Costs half of this claim's total presence. 'Assign permissions to memorized people: Landwindow$2 �fg haven/BuddyWnd$GroupSelector Trespassing �h 	Rummaging Theft 	Vandalism 5White permissions also apply to non-memorized people.i � � �jklmnopqr � � haven/GameUIst �uvwxy haven/MCache$Overlayz{ �| � �}x � � upd � � shared java/lang/Integer~ � � � � � � � � apow java/lang/Number���� entime %d:%02d until enabled�`�Z�� � take � � declaim bond � � haven/Window java/lang/String [Ljava/lang/Object; x y valueOf (I)Ljava/lang/Integer; format 9(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String; settext (Ljava/lang/String;)V fl a (II)V #(Lhaven/Coord;Ljava/lang/String;Z)V add +(Lhaven/Widget;Lhaven/Coord;)Lhaven/Widget; (LLandwindow;Lhaven/Coord;)V (Lhaven/Coord;I)V (ILjava/lang/String;)V haven/RichText render 8(Ljava/lang/String;I[Ljava/lang/Object;)Lhaven/RichText; haven/Widget tooltip Ljava/lang/Object;  (Lhaven/Widget;II)Lhaven/Widget; (LLandwindow;I)V haven/BuddyWnd "(LLandwindow;Ljava/lang/String;I)V pack ui 
Lhaven/UI; haven/UI sess Lhaven/Session; haven/Session glob Lhaven/Glob; 
haven/Glob 	getparent !(Ljava/lang/Class;)Lhaven/Widget; Lhaven/MapView; haven/MapView enol ([I)V haven/MCache getClass ()Ljava/lang/Class; ,(Lhaven/MCache;Lhaven/Coord;Lhaven/Coord;I)V disol intValue ()I doubleValue ()D set (D)V 
haven/Text� Line %(Ljava/lang/String;)Lhaven/Text$Line; (II)Lhaven/Coord; update haven/Text$Line 
land.cjava !  |       �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �     � �    � �  �      � �    � � �  �   8     +2� M+2� N� Y,-� �    �             � �  �   V     :*� � Y*� 	� 
*� � 
d`*� 	� *� � d`h� S� � �    �   
    # 9 $  � �  �   } 
    a*� � Y
*� � 
*� � 
d`*� � *� � d`h*� 	� 
*� � 
d`*� 	� *� � d`hdh� S� � �    �   
    ' ` (  � �  �   �     ?*� *� � .<*� M,�>6� #,2:� ~� � � ���ݱ    �   J �   � �  �   � � �  ��    � � �  ��   �   �       +  , $ - 8 , > .  � �  �  i 	   �*� Y� � *�
� *� � *� **+Z� � **,Z� 	� >**� Y�  � Y� � !� � �**� "Y*� Y,� � #� Y� � !� $�**� %Y� Y,� � &� Y� � !� %� '�**� (Y �)� *� Y� � !� +*� +,,� � -� .**� (Y �/� *� Y �� � !� 0�(**� Y1�  � Y� � !� � �*� 2**� (Yx3� *Z� 4� 5**� (Yx6� * �`� 4� 7**� (Yx8� *Z2`� 4� 9**� (Yx:� *`� 4� ;�d**� (Y �<� *� 4� =**� (Y �>� * �� 4� ?**� (Y �@� *#`� 4� A**� (Y �B� * �#`� 4� C*� CD,� � -� .�P*� YE�  � 4W�**� FY*� G� 4� H� �*� *� Y*I� J
� 4� S�*� *� Y*K� J
� 4� S�*� *� Y*L� J
� 4� S�*� *� Y*M� J
� 4� S�*� YN�  � 4W�*� O�    �   � &   F       C % G / H 9 I ; J [ K � ^ � _ � ` � a � b � c  d  e$ f: gT hm i� j� k� l� m� n� o� p  q r) x, yJ zi {� |� }� ~�   � �  �   {     S*� P**� Q� R� S� T� U*V� W� V� X�
YOYOYO� Y*� ZY*� UY� [W*� *� \� ]� ^�    �       �  �  � 4 � R �  � �  �   O     +*V� W� V� X�
YOYOYO� _*� ^� `*� a�    �       �  � & � * � � � �  �  �     +b� (,2� N,2� :*-� *� 	*� 2*� c� �+d� 2,2� e� f>,2� e� f6*� O*� � � *� � �+g� G*,2� e� f� h*,2� e� f� i*,2� e� f� j*,2� e� f� � � k*� � X+l� *� ',2� m� n� o� ?+p� 9,2� e� f>*� $q� Yl� SYp<l� S� � r� .�    �     +1} ��    � � �  �
; �   f    �  �  �  �  �   � $ � ( � 1 � ; � F � N � Y � ] � f � s � � � � � � � � � � � � � � � � � � � � � �  �  	    ?+*� 5� $**� � s� *� ^*� *� � t*� c�+*� 7� $**� � s� *� ^*� *� � t*� c�+*� 9� $**� � s� *� ^*� *� � t*� c�+*� ;� $**� � s� *� ^*� *� � t*� c�+*� =� *u� Y*� SY*� S� v�+*� ?� !*� ^**� Z� **� 	Z� � t*� c�+*� A� *w� � v�+*� C� *x� � v�+*� +� *y� � v�+*� 0� *z� � v�*+,-� {�    �    
)((( % �   � )   �  �  � $ � ( � ) � 1 � > � M � Q � R � Z � g � v � z � { � � � � � � � � � � � � � � � � � � � � � � � � � � � � � � � � � � �# �$ �, �6 �7 �> � � �  �        *� �    �        � �  �        *+Z� �    �        � �  �        *� �    �         �   � ~   2    }  F       "       H � 	 Z, � �B� 	code �  Landwindow$1 ����   4 �	 ! 3
 " 4	 1 5	 1 6
 7 8	  9	 ! :
 7 ;
  <	  = >
  ?
 7 @
 1 A	 1 B	 C D	 C E F G H
 I J
 K L
 M N	 O P
 Q R
 Q S
  T
 1 U
  V?�      
 7 W X Z this$0 LLandwindow; <init> (LLandwindow;Lhaven/Coord;)V Code LineNumberTable draw (Lhaven/GOut;)V StackMapTable > [ 
SourceFile Landwindow.java EnclosingMethod \ % ] # $ % ^ _ ` a ` b c d e f g f h ] i j k ` haven/Coord % l c m n o p q [ r s t s 
haven/TexI %s/%s java/lang/Object u v w x y z { | ~  � � � � � � � % � � � � � � � Landwindow$1 InnerClasses haven/Widget java/awt/Color 
Landwindow (Lhaven/Coord;Lhaven/Coord;)V (Lhaven/Coord;)V auth I acap 
haven/GOut chcolor (IIII)V z Lhaven/Coord; sz frect sub (II)Lhaven/Coord; x (II)V ()V 
access$000 (LLandwindow;)Lhaven/Tex; offline Z RED Ljava/awt/Color; WHITE java/lang/Integer valueOf (I)Ljava/lang/Integer; java/lang/String format 9(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String; 
haven/Text render Line 5(Ljava/lang/String;Ljava/awt/Color;)Lhaven/Text$Line; haven/Text$Line img Ljava/awt/image/BufferedImage; haven/Utils contrast "(Ljava/awt/Color;)Ljava/awt/Color; outline2 N(Ljava/awt/image/BufferedImage;Ljava/awt/Color;)Ljava/awt/image/BufferedImage; !(Ljava/awt/image/BufferedImage;)V 
access$002 $(LLandwindow;Lhaven/Tex;)Lhaven/Tex; div (I)Lhaven/Coord; aimage (Lhaven/Tex;Lhaven/Coord;DD)V 
land.cjava   ! "    # $      % &  '   #     *+� *,� �    (       K  ) *  '  8     �*� � =*� � >� �+ �� +� *� � + � �� *� � 	:� 
hl� 
+� Y� � +� *� � � N*� � � 	� � � :*� � Y� Y� SY� S� � � � � � � W+*� � *� �   �  �    +    � { ,B -7�  (   >    M  N  O  P  Q ) R 5 S @ T N U ] V a W k X � Y � [ � ]  .    � Y     !       O M } 	 0    1 2code )  Landwindow$PermBox ����   4 E	  
  	   	 ! "	  # $ %	 ! &	 ' (
 ) *
 ! +	 ! , - 0 fl I this$0 LLandwindow; <init> "(LLandwindow;Ljava/lang/String;I)V Code LineNumberTable changed (Z)V StackMapTable - 1 
SourceFile Landwindow.java    2   3 4 1 5 6 shared java/lang/Object 7 9 ; 7  < = > ? @ A B Landwindow$PermBox PermBox InnerClasses haven/CheckBox [LLandwindow$PermBox; (Ljava/lang/String;)V 
Landwindow perms a Z group GroupSelector Lhaven/BuddyWnd$GroupSelector; C haven/BuddyWnd$GroupSelector java/lang/Integer valueOf (I)Ljava/lang/Integer; wdgmsg ((Ljava/lang/String;[Ljava/lang/Object;)V bflags [I haven/BuddyWnd 
land.cjava                           4     *+� *,� *� �           3  4 
 5  6        �     l=*� � N-�66� -2:� � � �=����*� � Y*� � � 	� 
SY� 
S� *� � *� � � 	O�        �      �     "    9  :  ; & < . : 4 > X ? k @      D /      ! .  ' : 8 	codeentry     wdg Landwindow   ui/apower 
  