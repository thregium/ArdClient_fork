Haven Resource 1 src %  Amount.java /* Preprocessed source code */
import haven.*;

/* >tt: Amount */
public class Amount implements ItemInfo.InfoFactory {
    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
	int num = (Integer)args[1];
	return(new GItem.Amount(owner, num));
    }
}
code �  Amount ����   4 )
   
   
      <init> ()V Code LineNumberTable build ! Owner InnerClasses " Raw O(Lhaven/ItemInfo$Owner;Lhaven/ItemInfo$Raw;[Ljava/lang/Object;)Lhaven/ItemInfo; 
SourceFile Amount.java 	 
 java/lang/Integer # $ % haven/GItem$Amount Amount 	 & java/lang/Object ' haven/ItemInfo$InfoFactory InfoFactory haven/ItemInfo$Owner haven/ItemInfo$Raw intValue ()I haven/GItem (Lhaven/ItemInfo$Owner;I)V haven/ItemInfo amount.cjava !         	 
          *� �            �       2     -2� � 6� Y+� �       
            (    "    	    	    	    	codeentry    tt Amount   