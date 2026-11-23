package com.exoticworlds.core;

import com.mojang.serialization.Codec;

public record ShapeOption<T>(String key, int position, Codec<T> codec, T defaultValue) {
}
