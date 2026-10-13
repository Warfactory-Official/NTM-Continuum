package com.hbm.module;

import com.hbm.config.AutocalConfig;
import com.hbm.module.IParse.EnumStatementReturn;
import com.hbm.module.IParse.ParseContext;
import com.hbm.tileentity.network.RTTYSystem;

import java.util.Locale;

public class ParseMSES1 implements IParse {
    @Override
    public ReturnInfo eval(ParseContext ctx, String line, int index) {
        String lower = line.toLowerCase(Locale.US);

        if (lower.isBlank()) return new ReturnInfo(EnumStatementReturn.SKIP, index);

        switch (lower) {
            case "eval" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer is empty");
                String statement = substitute(ctx, ctx.readBuffer(), true);
                try { ctx.writeBuffer("" + Calculator.evaluateExpression(statement)); }
                catch (Throwable ignored) { return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer does not have a valid expression"); }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "evalr" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer is empty");
                String statement = substitute(ctx, ctx.readBuffer(), true);
                try { ctx.writeBuffer("" + (int) Math.round(Calculator.evaluateExpression(statement))); }
                catch (Throwable ignored) { return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer does not have a valid expression"); }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "rounddown", "floor" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer is empty");
                try { ctx.writeBuffer("" + (int) Math.floor(Double.parseDouble(ctx.readBuffer()))); }
                catch (Exception ignored) { return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer does not have a valid number"); }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "roundup", "ceil" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer is empty");
                try { ctx.writeBuffer("" + (int) Math.ceil(Double.parseDouble(ctx.readBuffer()))); }
                catch (Exception ignored) { return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer does not have a valid number"); }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "round", "nearest" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer is empty");
                try { ctx.writeBuffer("" + (int) Math.round(Double.parseDouble(ctx.readBuffer()))); }
                catch (Exception ignored) { return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer does not have a valid number"); }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
        }

        int space = line.indexOf(' ');
        String command;
        String args;
        if (space == -1) {
            command = lower;
            args = "";
        } else {
            command = lower.substring(0, space);
            args = line.substring(space + 1);
        }
        if (!command.equals(lower) && line.length() <= command.length() + 1) {
            return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Expected an argument");
        }

        switch (command) {
            case "dest", "#" -> { return new ReturnInfo(EnumStatementReturn.SKIP, index); }
            case "nop" -> { return new ReturnInfo(EnumStatementReturn.OK, index); }
            case "clockspeed" -> {
                try {
                    int speed = Integer.parseInt(args);
                    if (speed < 1 || speed > AutocalConfig.AUTOCAL_MAX_CLOCK) {
                        return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Invalid clock speed");
                    }
                    ctx.clockSpeed = speed;
                } catch (Throwable ignored) {
                    return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Not a valid number");
                }
                return new ReturnInfo(EnumStatementReturn.SKIP, index);
            }
            case "jmp" -> {
                String key = substitute(ctx, args, false);
                if (ctx.jmp.containsKey(key)) { ctx.current = ctx.jmp.get(key); return new ReturnInfo(EnumStatementReturn.OK, index); }
                return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Could not find jump destination");
            }
            case "jmpif" -> {
                if (!ctx.readBuffer().equals("true")) return new ReturnInfo(EnumStatementReturn.OK, index);
                String key = substitute(ctx, args, false);
                if (ctx.jmp.containsKey(key)) { ctx.current = ctx.jmp.get(key); return new ReturnInfo(EnumStatementReturn.OK, index); }
                return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Could not find jump destination");
            }
            case "jmpnot" -> {
                if (ctx.readBuffer().equals("true")) return new ReturnInfo(EnumStatementReturn.OK, index);
                String key = substitute(ctx, args, false);
                if (ctx.jmp.containsKey(key)) { ctx.current = ctx.jmp.get(key); return new ReturnInfo(EnumStatementReturn.OK, index); }
                return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Could not find jump destination");
            }
            case "endtick" -> { return new ReturnInfo(EnumStatementReturn.END_TICK, index); }
            case "shutdown" -> { return new ReturnInfo(EnumStatementReturn.SHUTDOWN, index); }
            case "load" -> { ctx.writeBuffer(ctx.getVariable(args)); return new ReturnInfo(EnumStatementReturn.OK, index); }
            case "save" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Cannot save variable, buffer is empty");
                ctx.setVariable(args, ctx.readBuffer());
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "buffer" -> { ctx.writeBuffer(args); return new ReturnInfo(EnumStatementReturn.OK, index); }
            case "eval" -> {
                try { ctx.writeBuffer("" + Calculator.evaluateExpression(substitute(ctx, args, true))); }
                catch (Throwable ignored) { return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Not a valid expression"); }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "evalr" -> {
                try { ctx.writeBuffer("" + (int) Math.round(Calculator.evaluateExpression(substitute(ctx, args, true)))); }
                catch (Throwable ignored) { return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Not a valid expression"); }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "concat" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Cannot concat, buffer is empty");
                ctx.writeBuffer(substitute(ctx, args, false));
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "eq" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer is empty");
                ctx.writeBuffer(ctx.readBuffer().equals(substitute(ctx, args, false)) ? "true" : "false");
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "gtb", "ltb", "geb", "leb" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Buffer is empty");
                try {
                    double buffer = Double.parseDouble(ctx.readBuffer());
                    double value = Double.parseDouble(substitute(ctx, args, false));
                    boolean result = switch (command) {
                        case "gtb" -> value > buffer;
                        case "ltb" -> value < buffer;
                        case "geb" -> value >= buffer;
                        default -> value <= buffer;
                    };
                    ctx.writeBuffer(result ? "true" : "false");
                } catch (Exception ignored) {
                    return new ReturnInfo(EnumStatementReturn.PARAMETER_ERROR, index, "Not a valid number");
                }
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "send" -> {
                if (ctx.readBuffer().isEmpty()) return new ReturnInfo(EnumStatementReturn.UNDEFINED, index, "Cannot send, buffer is empty");
                RTTYSystem.broadcast(ctx.world, substitute(ctx, args, false), ctx.readBuffer());
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
            case "listen" -> {
                RTTYSystem.RTTYChannel channel = RTTYSystem.listen(ctx.world, substitute(ctx, args, false));
                if (channel != null) ctx.writeBuffer(channel.signal + "");
                return new ReturnInfo(EnumStatementReturn.OK, index);
            }
        }
        return new ReturnInfo(EnumStatementReturn.UNRECOGNIZED_COMMAND, index);
    }

    public String substitute(ParseContext ctx, String statement, boolean forceNumber) {
        if (!statement.contains("$")) return statement;
        statement = "¤" + statement + "¤";
        String[] frags = statement.split("\\$");
        if (frags.length % 2 == 0 || frags.length < 3) return statement.substring(1, statement.length() - 1);
        for (int i = 1; i < frags.length; i += 2) {
            if (frags[i].equals("buffer")) frags[i] = ctx.readBuffer();
            else {
                String variable = ctx.getVariable(frags[i]);
                if (forceNumber && variable.isEmpty()) variable = "0";
                frags[i] = variable;
            }
        }
        String ret = String.join("", frags);
        return ret.substring(1, ret.length() - 1);
    }

    @Override
    public void generateJumpPoints(ParseContext ctx, String line, int index) {
        if (line.startsWith("dest ") && line.length() > 5) ctx.jmp.put(line.substring(5), index);
    }
}
