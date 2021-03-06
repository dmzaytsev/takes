/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.facets.fallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.EqualsAndHashCode;
import org.takes.Request;
import org.takes.Take;
import org.takes.Takes;
import org.takes.ts.TsFixed;

/**
 * Fallback takes.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 0.1
 */
@EqualsAndHashCode(of = { "origin", "fallback" })
public final class TsFallback implements Takes {

    /**
     * Original takes.
     */
    private final transient Takes origin;

    /**
     * Fallback takes.
     */
    private final transient Fallback fallback;

    /**
     * Ctor.
     * @param org Original
     * @param fbk Fallback
     */
    public TsFallback(final Takes org, final Take fbk) {
        this(org, new TsFixed(fbk));
    }

    /**
     * Ctor.
     * @param org Original
     * @param fbk Fallback
     */
    public TsFallback(final Takes org, final Takes fbk) {
        this(
            org,
            new Fallback() {
                @Override
                public Take take(final RqFallback request) throws IOException {
                    return fbk.route(request);
                }
            }
        );
    }

    /**
     * Ctor.
     * @param org Original
     * @param fbk Fallback
     */
    public TsFallback(final Takes org, final Fallback fbk) {
        this.origin = org;
        this.fallback = fbk;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public Take route(final Request request) throws IOException {
        Take take;
        try {
            take = new TkFallback(
                this.origin.route(request), this.fallback, request
            );
        // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Throwable ex) {
            take = this.fallback.take(TsFallback.req(request, ex));
        }
        return take;
    }

    /**
     * Make a request from original one and throwable.
     * @param req Request
     * @param err Error
     * @return Request
     */
    private static RqFallback req(final Request req, final Throwable err) {
        return new RqFallback() {
            @Override
            public Throwable throwable() {
                return err;
            }
            @Override
            public List<String> head() throws IOException {
                return req.head();
            }
            @Override
            public InputStream body() throws IOException {
                return req.body();
            }
        };
    }

}
