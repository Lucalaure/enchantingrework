package lucalaure1007.enchantingrework.network;

import lucalaure1007.enchantingrework.EnchantingRework;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sends the server's config to a joining player so tooltips show the server's tiers and limits. */
public record ConfigSyncPayload(String json) implements CustomPacketPayload {
	public static final Type<ConfigSyncPayload> TYPE = new Type<>(EnchantingRework.id("config"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ConfigSyncPayload::json,
		ConfigSyncPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
