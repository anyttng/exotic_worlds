package com.exoticworlds.core.option;

import com.mojang.serialization.Codec;

public record ShapeOption<T>(String key, int position, Codec<T> codec, T defaultValue) {
}
