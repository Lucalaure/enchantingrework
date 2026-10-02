package lucalaure1007.enchantingrework.table;

/** Why the table can or can't enchant right now. Sent to the client by ordinal. */
public enum TableStatus {
	READY,
	NO_ITEM,
	NOT_ENCHANTABLE,
	TIER_LOCKED,
	NO_MATCH,
	NEED_LAPIS,
	NEED_CATALYST,
	ALREADY_ENCHANTED,
	NO_OFFER;

	public String translationKey() {
		return "enchantingrework.status." + this.name().toLowerCase(java.util.Locale.ROOT);
	}

	public static TableStatus byId(int id) {
		TableStatus[] values = values();
		return id >= 0 && id < values.length ? values[id] : NO_ITEM;
	}
}
