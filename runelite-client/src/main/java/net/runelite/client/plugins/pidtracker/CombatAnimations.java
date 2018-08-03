package net.runelite.client.plugins.pidtracker;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.runelite.api.AnimationID;

public class CombatAnimations
{
	public static final Set<Integer> MELEE = ImmutableSet.of(
		AnimationID.WHIP,
		AnimationID.KICK,
		AnimationID.BOX,
		AnimationID.CLAWS_SLASH,
		AnimationID.CLAWS_STAB,
		AnimationID.WARHAMMER,
		AnimationID.DRAGON_WARHAMMER_SPEC,
		AnimationID.HASTA_SLASH,
		AnimationID.HASTA_STAB,
		AnimationID.HASTA_CRUSH,
		AnimationID.SPEAR_SLASH,
		AnimationID.SPEAR_STAB,
		AnimationID.SPEAR_CRUSH,
		AnimationID.GODSWORD_SLASH,
		AnimationID.GODSWORD_CRUSH,
		AnimationID.GODSWORD_SPEC_BANDOS,
		AnimationID.GODSWORD_SPEC_SARADOMIN,
		AnimationID.GODSWORD_SPEC_ARMADYL,
		AnimationID.GODSWORD_SPEC_ZAMORAK,
		AnimationID.SCYTHE_OF_VITUR_SLASH,
		AnimationID.TWO_HANDED_SWORD_SLASH,
		AnimationID.TWO_HANDED_SWORD_CRUSH,
		AnimationID.SWORD_SLASH,
		AnimationID.SWORD_STAB,
		AnimationID.DRAGON_LONGSWORD_SPECIAL_ATTACK,
		AnimationID.DRAGON_SCIMITAR_SPECIAL_ATTACK,
		AnimationID.BATTLEAXE_SLASH,
		AnimationID.BATTLEAXE_CRUSH,
		AnimationID.LEAF_BLADED_BATTLEAXE_SLASH,
		AnimationID.LEAF_BLADED_BATTLEAXE_CRUSH,
		AnimationID.DRAGON_DAGGER_STAB,
		AnimationID.DRAGON_DAGGER_SLASH,
		AnimationID.MACE_CRUSH,
		AnimationID.MACE_STAB,
		AnimationID.PICKAXE_STAB,
		AnimationID.PICKAXE_CRUSH,
		AnimationID.AXE_SLASH,
		AnimationID.AXE_CRUSH,
		AnimationID.DHAROKS_GREATAXE_SLASH,
		AnimationID.DHAROKS_GREATAXE_CRUSH,
		AnimationID.VERACS_FLAIL,
		AnimationID.GUTHANS_SPEAR_SLASH,
		AnimationID.GUTHANS_SPEAR_STAB,
		AnimationID.GUTHANS_SPEAR_CRUSH,
		AnimationID.TORAGS_HAMMERS,
		AnimationID.BLUDGEON,
		AnimationID.GHRAZI_RAPIER
	);

	public static final Set<Integer> MAGE = ImmutableSet.of(
		AnimationID.TRIDENT,
		AnimationID.ANCIENT_MAGIC_SPELL_SINGLE,
		AnimationID.ANCIENT_MAGIC_SPELL_MULTI,
		AnimationID.CAST_IBAN_SPELL,
		AnimationID.CAST_CRUMBLE_UNDEAD_SPELL,
		AnimationID.CAST_MAGIC_DART_SPELL,
		AnimationID.CAST_FLAMES_OF_ZAMORAK_SPELL,
		AnimationID.CAST_BIND_SPELL,
		AnimationID.CAST_ELEMENTAL_SPELL,
		AnimationID.CAST_ELEMENTAL_SPELL_WITH_STAFF,
		AnimationID.CAST_WAVE_SPELL,
		AnimationID.CAST_WAVE_SPELL_WITH_STAFF,
		AnimationID.CAST_SURGE_SPELL
	);

	public static final Set<Integer> RANGE = ImmutableSet.of(
		AnimationID.BLOWPIPE,
		AnimationID.BOW,
		AnimationID.CHINCHOMPA_THROW,
		AnimationID.KNIFE_THROW,
		AnimationID.CROSSBOW,
		AnimationID.KARILS_CROSSBOW,
		AnimationID.DART_THROW
	);

}
