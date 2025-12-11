package com.jswone.commerce.core.util;

import com.google.gson.*;
import com.jswone.commerce.core.constants.BuyAgainConstants;
import com.jswone.commerce.core.model.Uom;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class UomDeserializer implements JsonDeserializer<Uom> {

    @Override
    public Uom deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        double value =
                jsonObject.get(BuyAgainConstants.VALUE).isJsonNull()
                        ? 0.0
                        : jsonObject.get(BuyAgainConstants.VALUE).getAsDouble();
        DecimalFormat threeDecimalFormat = new DecimalFormat("0.000");
        threeDecimalFormat.setRoundingMode(RoundingMode.FLOOR);
        String formattedValue = threeDecimalFormat.format(value);
        value = Double.parseDouble(formattedValue);

        String priceLabel =
                jsonObject.get(BuyAgainConstants.PRICE_LABEL).isJsonNull()
                        ? StringUtils.EMPTY
                        : jsonObject.get(BuyAgainConstants.PRICE_LABEL).getAsString();
        String unit =
                jsonObject.get(BuyAgainConstants.UNIT).isJsonNull()
                        ? StringUtils.EMPTY
                        : jsonObject.get(BuyAgainConstants.UNIT).getAsString();
        String label =
                jsonObject.get(BuyAgainConstants.LABEL).isJsonNull()
                        ? StringUtils.EMPTY
                        : jsonObject.get(BuyAgainConstants.LABEL).getAsString();
        return new Uom(unit, label, value, priceLabel);
    }
}
