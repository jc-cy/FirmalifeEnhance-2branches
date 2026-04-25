package com.g1739.firmalifegreenhousepatch.client;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ConfigScreenTranslationHelper
{
    private static final String CATEGORY_PREFIX = FirmalifeGreenhousePatch.MOD_ID + ".config.category.";

    private ConfigScreenTranslationHelper() {}

    @Nullable
    public static Component getCategoryTitle(String path)
    {
        final String directKey = CATEGORY_PREFIX + path;
        if (I18n.exists(directKey))
        {
            return Component.translatable(directKey);
        }

        final String snakeCaseKey = CATEGORY_PREFIX + camelToSnake(path);
        if (!snakeCaseKey.equals(directKey) && I18n.exists(snakeCaseKey))
        {
            return Component.translatable(snakeCaseKey);
        }
        return null;
    }

    private static String camelToSnake(String value)
    {
        final StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++)
        {
            final char c = value.charAt(i);
            if (Character.isUpperCase(c))
            {
                if (builder.length() > 0)
                {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(c));
            }
            else
            {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
