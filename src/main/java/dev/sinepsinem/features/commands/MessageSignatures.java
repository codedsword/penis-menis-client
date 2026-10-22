package dev.sinepsinem.features.commands;

import dev.sinepsinem.util.chat.Signature;
import dev.sinepsinem.util.chat.SimpleSignature;

public class MessageSignatures {
    public static final Signature GENERAL = SimpleSignature.fromLong(-1L);
    public static final Signature SUCCESS = SimpleSignature.fromLong(0L);
    public static final Signature FAIL    = SimpleSignature.fromLong(1L);
}