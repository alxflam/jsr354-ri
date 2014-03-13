/*
 * CREDIT SUISSE IS WILLING TO LICENSE THIS SPECIFICATION TO YOU ONLY UPON THE CONDITION THAT YOU
 * ACCEPT ALL OF THE TERMS CONTAINED IN THIS AGREEMENT. PLEASE READ THE TERMS AND CONDITIONS OF THIS
 * AGREEMENT CAREFULLY. BY DOWNLOADING THIS SPECIFICATION, YOU ACCEPT THE TERMS AND CONDITIONS OF
 * THE AGREEMENT. IF YOU ARE NOT WILLING TO BE BOUND BY IT, SELECT THE "DECLINE" BUTTON AT THE
 * BOTTOM OF THIS PAGE. Specification: JSR-354 Money and Currency API ("Specification") Copyright
 * (c) 2012-2013, Credit Suisse All rights reserved.
 */
package org.javamoney.moneta.format.internal;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Logger;

import javax.money.MonetaryAmount;
import javax.money.format.AmountStyle;
import javax.money.format.AmountFormatSymbols;
import javax.money.format.MonetaryParseException;

/**
 * {@link FormatToken} which allows to format a {@link Number} type.
 * 
 * @author Anatole Tresch
 */
final class AmountNumberToken implements FormatToken {

	private AmountStyle style;
	private String partialNumberPattern;
	private DecimalFormat parseFormat;
    private DecimalFormat formatFormat;
	private StringGrouper numberGroup;

	public AmountNumberToken(AmountStyle style, String partialNumberPattern) {
		if (style == null) {
			throw new IllegalArgumentException("style is required.");
		}
		this.style = style;
		this.partialNumberPattern = partialNumberPattern;
		initDecimalFormats();
	}

	private void initDecimalFormats() {
        formatFormat = (DecimalFormat) DecimalFormat.getInstance(style
				.getLocale());
        parseFormat = (DecimalFormat) DecimalFormat.getInstance(style
                                                                         .getLocale());
		DecimalFormatSymbols symbols = formatFormat.getDecimalFormatSymbols();
		AmountFormatSymbols syms = style.getSymbols();
		if (syms != null) {
			symbols.setDecimalSeparator(syms.getDecimalSeparator());
			symbols.setDigit(syms.getDigit());
			symbols.setExponentSeparator(syms.getExponentSeparator());
			symbols.setMinusSign(syms.getMinusSign());
			symbols.setPatternSeparator(syms.getPatternSeparator());
			symbols.setZeroDigit(syms.getZeroDigit());
            formatFormat.setDecimalFormatSymbols(symbols);
            parseFormat.setDecimalFormatSymbols(symbols);
		}
        formatFormat.applyPattern(this.partialNumberPattern);
        parseFormat.applyPattern(this.partialNumberPattern.trim());
	}

	/**
	 * Access the underlying amount style. Note that the pattern that
	 * corresponds to this {@link AmountNumberToken} instance may be only a
	 * partial pattern of the full pattern returned by
	 * {@link AmountStyle#getPattern()}.
	 * 
	 * @return the {@link AmountStyle}.
	 */
	public AmountStyle getAmountStyle() {
		return style;
	}

	/**
	 * Get the number pattern used by this {@link AmountNumberToken} token. This
	 * pattern can be a partial pattern, of the full pattern in place.
	 * 
	 * @return the number pattern used, never {@code null}.
	 */
	public String getNumberPattern() {
		return this.partialNumberPattern;
	}

	@Override
	public void print(Appendable appendable, MonetaryAmount amount)
			throws IOException {
		int digits = amount.getCurrency().getDefaultFractionDigits();
		this.formatFormat.setMinimumFractionDigits(digits);
		this.formatFormat.setMaximumFractionDigits(digits);
		if (this.style.getGroupingSizes().length == 0) {
			appendable.append(this.formatFormat.format(amount.getNumber()
					.numberValue(BigDecimal.class)));
			return;
		}
		this.formatFormat.setGroupingUsed(false);
		String preformattedValue = this.formatFormat.format(amount.getNumber()
				.numberValue(BigDecimal.class));
		String[] numberParts = splitNumberParts(this.formatFormat,
				preformattedValue);
		if (numberParts.length != 2) {
			appendable.append(preformattedValue);
		} else {
			if (numberGroup == null) {
				char[] groupChars = style.getSymbols().getGroupingSeparators();
				if (groupChars.length == 0) {
					groupChars = new char[] { this.formatFormat
							.getDecimalFormatSymbols().getGroupingSeparator() };
				}
				numberGroup = new StringGrouper(groupChars,
						style.getGroupingSizes());
			}
			preformattedValue = numberGroup.group(numberParts[0])
					+ this.formatFormat.getDecimalFormatSymbols()
							.getDecimalSeparator() + numberParts[1];
			appendable.append(preformattedValue);
		}
	}

	private String[] splitNumberParts(DecimalFormat format,
			String preformattedValue) {
		int index = preformattedValue.indexOf(format.getDecimalFormatSymbols()
				.getDecimalSeparator());
		if (index < 0) {
			return new String[] { preformattedValue };
		}
		return new String[] { preformattedValue.substring(0, index),
				preformattedValue.substring(index + 1) };
	}

	@Override
	public void parse(ParseContext context) throws MonetaryParseException {
		String token = context.lookupNextToken();
		if(token != null && !context.isComplete()) {
			parseToken(context, token);
            if(context.hasError()){
                throw new MonetaryParseException(context.getErrorMessage(), context.getInput(), context.getIndex());
            }
		}
        else{
            context.setError();
            context.setErrorMessage("Number expected.");
        }
	}

	private void parseToken(ParseContext context, String token) {
		try {
			Number number = this.parseFormat.parse(token);
			if (number != null) {
				context.setParsedNumber(number);
				context.consume(token);
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).finest(
					"Could not parse amount from: " + token);
            context.setError();
            context.setErrorMessage(e.getMessage());
		}
	}

}
