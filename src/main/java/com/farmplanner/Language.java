package com.farmplanner;

/** Taal van de teksten in het paneel en het plan. */
public enum Language
{
	ENGLISH("English"),
	DUTCH("Nederlands");

	private final String label;

	Language(String label)
	{
		this.label = label;
	}

	/** Kies de tekst in deze taal. */
	public String t(String english, String dutch)
	{
		return this == DUTCH ? dutch : english;
	}

	/** Getallen met duizendtal-scheiding in de stijl van deze taal (1,234 of 1.234). */
	public String number(double value)
	{
		return String.format(this == DUTCH ? java.util.Locale.forLanguageTag("nl") : java.util.Locale.ENGLISH,
			"%,.0f", value);
	}

	@Override
	public String toString()
	{
		return label;
	}
}
