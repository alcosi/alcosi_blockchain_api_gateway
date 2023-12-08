/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.alcosi.nft.apigateway.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin(name = "CustomMessagePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"m", "msg", "message"})
@PerformanceSensitive("allocation")
public class MaskLogEventFactory extends LogEventPatternConverter {
    final static Integer MAX_SIZE = 100;
    public static  Set<Pattern> FILE_PATTERNS = Stream.of(
            Pattern.compile("([\\da-fA-F]{" + MAX_SIZE + ",})"),
            Pattern.compile("([\\da-zA-Z+\\/]{" + MAX_SIZE + ",}={0,3})")
    ).collect(Collectors.toSet());

    MaskLogEventFactory(final String[] options) {
        super("m", "m");
    }

    public static MaskLogEventFactory newInstance(final String[] options) {
        return new MaskLogEventFactory(options);
    }

    @Override
    public void format(LogEvent event, StringBuilder outputMessage) {
        try {
            outputMessage.append(mask(event.getMessage().getFormattedMessage()));
        } catch (Exception e) {
            outputMessage.append("EXCEPTION IN LOGGER!");
        }
    }

    private String mask(String message) {
        try {
            return getAllPatterns()
                    .reduce(message, (msg, p) -> {
                        Matcher m = p.matcher(msg);
                        return m.find()
                                ? replaceAll(m)
                                : msg;
                    }, (one, second) -> one);
        } catch (Throwable t) {
            return "EXCEPTION IN LOGGER!" + message;
        }
    }

    private String replaceAll(Matcher m) {
        return m.replaceAll("<TOO BIG>");
    }

    private static Stream<Pattern> getAllPatterns() {
        return Stream.of(FILE_PATTERNS).flatMap(Collection::stream);
    }
}
