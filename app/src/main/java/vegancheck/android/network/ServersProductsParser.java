package vegancheck.android.network;

import java.text.ParseException;

import vegancheck.android.App;
import vegancheck.android.Product;

public final class ServersProductsParser {
    private static final char SEPARATOR_CHAR = '§';
    private static final String SEPARATOR_STRING = Character.toString(SEPARATOR_CHAR);
    private static final int VALID_SEPARATORS_COUNT = 7;

    private ServersProductsParser() {
    }

    /**
     * @return fully initialized product (isFullyInitialized() == true), never null
     */
    public static Product parse(final String encodedProduct, final String barcode) throws ParseException {
        App.logDebug(ServersProductsParser.class, encodedProduct);

        final String validEncodedProduct = validate(encodedProduct);

        final String[] arguments = validEncodedProduct.split(SEPARATOR_STRING);
        App.assertCondition(
                arguments.length >= VALID_SEPARATORS_COUNT + 1,
                "arguments.length=(" + arguments.length + "), "
                + "VALID_SEPARATORS_COUNT=(" + VALID_SEPARATORS_COUNT + ")" );

        return parse(arguments, barcode);
    }

    private static String validate(final String encodedProduct) throws ParseException {
        if (encodedProduct == null) {
            throw new ParseException("null encodedProduct given", 0);
        }

        final int separatorsCount = getSeparatorsCountIn(encodedProduct);
        if (separatorsCount < VALID_SEPARATORS_COUNT) {
            throw new ParseException("encodedProduct has invalid number of separators", 0);
        } else if (separatorsCount > VALID_SEPARATORS_COUNT) {
            App.logDebug(ServersProductsParser.class, "server gave a product with more field than the client can read!");
        }

        String validatedEncodedProduct = encodedProduct;
        if (validatedEncodedProduct.charAt(0) == SEPARATOR_CHAR) {
            validatedEncodedProduct = " " + validatedEncodedProduct;
        }

        if (validatedEncodedProduct.charAt(validatedEncodedProduct.length() - 1) == SEPARATOR_CHAR) {
            validatedEncodedProduct += " ";
        }

        return validatedEncodedProduct;
    }

    private static int getSeparatorsCountIn(final String string) {
        return string.length() - string.replace(SEPARATOR_STRING, "").length();
    }

    private static Product parse(final String[] arguments, final String barcode) throws ParseException {
        final String name = arguments[0];
        final boolean isVegan = arguments[1].equals("0");
        final boolean isVegetarian = arguments[2].equals("0");
        final boolean wasTestedOnAnimals = arguments[3].equals("1");
        final String company = arguments[5];
        final boolean isVerified = arguments[7].equals("1");

        final Product.Status status;
        if (isVegan) {
            status = Product.Status.VEGAN;
        } else if (isVegetarian) {
            status = Product.Status.VEGETARIAN;
        } else {
            status = Product.Status.NOT_VEGETARIAN;
        }

        try {
            return new Product(
                    barcode,
                    name,
                    company,
                    status,
                    wasTestedOnAnimals,
                    isVerified);
        } catch (final IllegalArgumentException e) {
            App.logInfo(
                    ServersProductsParser.class,
                    "a product refused to be created, message:\n" + e.getMessage());
            throw new ParseException("a product refused to be created", -1);
        }
    }
}
