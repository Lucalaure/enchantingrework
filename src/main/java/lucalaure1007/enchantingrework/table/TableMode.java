package lucalaure1007.enchantingrework.table;

public enum TableMode {
	/** Nothing to offer. */
	NONE,
	/** A catalyst picks the main enchantment. */
	CATALYST,
	/** No catalyst: a cheap vanilla-style random roll. */
	GAMBLE;

	public static TableMode byId(int id) {
		TableMode[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
