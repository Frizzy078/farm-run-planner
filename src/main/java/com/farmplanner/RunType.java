package com.farmplanner;

public enum RunType
{
	HERB("Herb run"),
	TREE("Tree run"),
	FRUIT_TREE("Fruit tree run");

	private final String label;

	RunType(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
