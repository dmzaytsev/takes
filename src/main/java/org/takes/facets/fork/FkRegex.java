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
package org.takes.facets.fork;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import org.takes.Request;
import org.takes.Take;
import org.takes.Takes;
import org.takes.rq.RqHref;
import org.takes.tk.TkText;
import org.takes.ts.TsFixed;

/**
 * Fork by regular expression pattern.
 *
 * <p>Use this class in combination with {@link org.takes.facets.fork.TsFork},
 * for example:
 *
 * <pre> Takes takes = new TsFork(
 *   new FkRegex("/home", new TsHome()),
 *   new FkRegex("/account", new TsAccount())
 * );</pre>
 *
 * <p>Each instance of {@link org.takes.facets.fork.FkRegex} is being
 * asked only once by {@link org.takes.facets.fork.TsFork} whether the
 * request is good enough to be processed. If the request is suitable
 * for this particular fork, it will return the relevant
 * {@link org.takes.Take}.
 *
 * <p>Also, keep in mind that the second argument of the constructor may
 * be of type {@link org.takes.facets.fork.Target} and accept an
 * instance of {@link org.takes.facets.fork.RqRegex}, which makes it very
 * convenient to reuse regular expression matcher, for example:
 *
 * <pre> Takes takes = new TsFork(
 *   new FkRegex(
 *     "/file(.*)",
 *     new Target&lt;RqRegex&gt;() {
 *       &#64;Override
 *       public Take route(final RqRegex req) {
 *         // Here we immediately getting access to the
 *         // matcher that was used during parsing of
 *         // the incoming request
 *         final String file = req.matcher().group(1);
 *       }
 *     }
 *   )
 * );</pre>
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 0.4
 * @see org.takes.facets.fork.TsFork
 * @see org.takes.facets.fork.Target
 */
@EqualsAndHashCode(of = { "pattern", "target" })
public final class FkRegex implements Fork.AtTake {

    /**
     * Pattern.
     */
    private final transient Pattern pattern;

    /**
     * Target.
     */
    private final transient Target<RqRegex> target;

    /**
     * Ctor.
     * @param ptn Pattern
     * @param text Text
     */
    public FkRegex(final String ptn, final String text) {
        this(Pattern.compile(ptn), new TsFixed(new TkText(text)));
    }

    /**
     * Ctor.
     * @param ptn Pattern
     * @param take Take
     */
    public FkRegex(final String ptn, final Take take) {
        this(ptn, new TsFixed(take));
    }

    /**
     * Ctor.
     * @param ptn Pattern
     * @param tks Takes
     */
    public FkRegex(final String ptn, final Takes tks) {
        this(Pattern.compile(ptn), tks);
    }

    /**
     * Ctor.
     * @param ptn Pattern
     * @param tks Takes
     */
    public FkRegex(final Pattern ptn, final Takes tks) {
        this(
            ptn,
            new Target<RqRegex>() {
                @Override
                public Take route(final RqRegex req) throws IOException {
                    return tks.route(req);
                }
            }
        );
    }

    /**
     * Ctor.
     * @param ptn Pattern
     * @param tgt Takes
     */
    public FkRegex(final String ptn, final Target<RqRegex> tgt) {
        this(Pattern.compile(ptn), tgt);
    }

    /**
     * Ctor.
     * @param ptn Pattern
     * @param tgt Takes
     */
    public FkRegex(final Pattern ptn, final Target<RqRegex> tgt) {
        this.pattern = ptn;
        this.target = tgt;
    }

    @Override
    public Iterator<Take> route(final Request req) throws IOException {
        final Matcher matcher = this.pattern.matcher(
            new RqHref(req).href().path()
        );
        final Collection<Take> list = new ArrayList<Take>(1);
        if (matcher.matches()) {
            list.add(
                this.target.route(
                    new RqRegex() {
                        @Override
                        public Matcher matcher() {
                            return matcher;
                        }
                        @Override
                        public List<String> head() throws IOException {
                            return req.head();
                        }
                        @Override
                        public InputStream body() throws IOException {
                            return req.body();
                        }
                    }
                )
            );
        }
        return list.iterator();
    }

}
