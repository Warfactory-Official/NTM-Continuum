package com.hbm.modules;

import dev.golem.core.data.Tags;

import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IParse {
    ReturnInfo eval(ParseContext ctx, String line, int index);
    void generateJumpPoints(ParseContext ctx, String line, int index);

    class ParseContext {
        public static final int MAX_BUFFER_LENGTH = 256;
        public static final int MAX_STACK_SIZE = 256;
        public static final Codec<List<String>> SCRIPT_CODEC = Codec.STRING.listOf();
        public static final Codec<Map<String, String>> VARIABLES_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

        public Level world;
        public final Map<String, String> variables = new HashMap<>();
        public final HashMap<String, Integer> jmp = new HashMap<>();

        private String buffer = "";
        private final String[] stack = new String[MAX_STACK_SIZE];
        private int stackSize = 0;
        public String splitString = ";";
        public int clockSpeed = 1;
        public int current = 0;

        public ParseContext(Level world) {
            this.world = world;
            Arrays.fill(this.stack, "");
        }

        public String readBuffer() {
            return this.buffer;
        }

        /** Sets the buffer and imposes the old 256-character restriction. */
        public boolean writeBuffer(String buffer) {
            if (buffer.length() > MAX_BUFFER_LENGTH) {
                this.buffer = buffer.substring(0, MAX_BUFFER_LENGTH);
                return false;
            }
            this.buffer = buffer;
            return true;
        }

        public boolean push(String line) {
            if (stackSize >= MAX_STACK_SIZE) return false;
            if (line.length() > MAX_BUFFER_LENGTH) line = line.substring(0, MAX_BUFFER_LENGTH);
            stack[stackSize] = line;
            stackSize++;
            return true;
        }

        public String pop() {
            if (stackSize <= 0) return null;
            if (stackSize > MAX_STACK_SIZE) stackSize = MAX_STACK_SIZE;
            String ret = stack[stackSize - 1];
            stack[stackSize - 1] = "";
            stackSize--;
            return ret;
        }

        public String peek() {
            if (stackSize <= 0) return null;
            if (stackSize > MAX_STACK_SIZE) stackSize = MAX_STACK_SIZE;
            return stack[stackSize - 1];
        }

        public String getVariable(String name) {
            return variables.getOrDefault(name, "");
        }

        public void setVariable(String name, String value) {
            variables.put(name, value);
        }

        public void turnOff() {
            this.clockSpeed = 1;
            this.current = 0;
            this.buffer = "";
            if (!this.variables.isEmpty()) this.variables.clear();
        }

        public void readData(DataIn input, String[] script, IParse parser) {
            current = input.getIntOr("current", 0);
            clockSpeed = input.getIntOr("clockSpeed", 0);
            buffer = input.getStringOr("buffer", "");
            splitString = input.getStringOr("splitString", "");
            variables.clear();
            input.read("variables", VARIABLES_CODEC).ifPresent(variables::putAll);

            stackSize = Math.max(0, Math.min(MAX_STACK_SIZE, input.getIntOr("stackSize", 0)));
            for (int i = 0; i < stackSize; i++) stack[i] = input.getStringOr("st" + i, "");
            jmp.clear();
            for (int i = 0; i < script.length; i++) parser.generateJumpPoints(this, script[i], i);
        }

        public void writeData(DataOut output) {
            output.putInt("current", current);
            output.putInt("clockSpeed", clockSpeed);
            output.putString("buffer", buffer);
            output.putString("splitString", splitString);
            output.store("variables", VARIABLES_CODEC, variables);

            output.putInt("stackSize", stackSize);
            for (int i = 0; i < stackSize; i++) output.putString("st" + i, stack[i]);
        }
    }

    record ReturnInfo(EnumStatementReturn type, int line, String extraInfo) {
        public ReturnInfo(EnumStatementReturn type, int line) {
            this(type, line, "");
        }
    }

    enum EnumStatementReturn {
        OK,
        UNRECOGNIZED_COMMAND,
        PARAMETER_ERROR,
        END_TICK,
        SHUTDOWN,
        SKIP,
        UNDEFINED,
        STACK_EXCEEDED
    }
}
