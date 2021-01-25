/**
 * Author: Sven Gothel <sgothel@jausoft.com>
 * Copyright (c) 2021 Gothel Software e.K.
 * Copyright (c) 2019 Gothel Software e.K.
 * Copyright (c) 2019 JogAmp Community.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.jau.sec;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jau.io.IOUtil;
import org.jau.sys.Debug;

/**
 * Utility class to produce secure hash (SHA) sums over diverse input sources.
 * <p>
 * See {@link #updateDigest(MessageDigest, List)}
 * </p>
 * <p>
 * This implementation is being utilized at JogAmp build time to produce various
 * SHA sums over sources, class files and native libraries to ensure their identity.
 * See {@link JauVersion#getImplementationSHASources()},
 * {@link JauVersion#getImplementationSHAClasses()}
 * and {@link JauVersion#getImplementationSHANatives()}.
 * </p>
 * <p>
 * {@link JauVersion#getImplementationSHASources()} for module gluegen is produced via:
 * <pre>
 * java -cp build/gluegen-rt.jar com.jogamp.common.util.SHASum --algorithm 256 --exclude ".*\\.log" --exclude "make/lib/toolchain" src jcpp/src make
 * </pre>
 * </p>
 * @see #SHASum(MessageDigest, List, List, List)
 * @see #compute(boolean)
 * @see org.jau.pkg.TempJarSHASum
 * @see #main(String[])
 */
public class SHASum {
    private static final boolean DEBUG = Debug.debug("SHASum");

    /**
     * {@link MessageDigest#update(byte[], int, int) Updates} the given {@code digest}
     * with the bytes contained by the files denoted by the given {@code filenames} in the given order.
     * <p>
     * To retrieve the list of all files traversing through directories, one may use {@link IOUtil#filesOf(List, List, List)}.
     * </p>
     * <p>
     * The SHA implementation is sensitive to the order of input bytes and hence the given filename order.
     * </p>
     * <p>
     * It is advised to pass given list of filenames in lexicographically sorted order to ensure reproducible outcome across all platforms,
     * one may use {@link #sort(ArrayList)}.
     * </p>
     * <p>
     * As an example, one could write
     * <pre>
     * final MessageDigest digest = ...;
     * final long totalBytes = updateDigest(digest, sort(IOUtil.filesOf(Arrays.asList("sources"), null, null)));
     * </pre>
     * </p>
     * @param digest to be updated digest
     * @param filenames list of filenames denoting files, which bytes will be used to update the digest
     * @return total number of bytes read.
     * @throws FileNotFoundException see {@link FileInputStream#FileInputStream(String)}
     * @throws IOException see {@link InputStream#read(byte[])}
     */
    public static long updateDigest(final MessageDigest digest, final List<String> filenames) throws IOException {
        long numBytes = 0;
        final byte buffer[] = new byte[4096]; // avoid Platform.getMachineDataInfo().pageSizeInBytes() due to native dependency
        for(int i=0; i<filenames.size(); i++) {
            final InputStream in = new BufferedInputStream(new FileInputStream(filenames.get(i)));
            try {
                while (true) {
                    int count;
                    if ((count = in.read(buffer)) == -1) {
                        break;
                    }
                    digest.update(buffer, 0, count);
                    numBytes += count;
                }
            } finally {
                in.close();
            }
        }
        return numBytes;
    }

    /**
     * Simple helper to print the given byte-array into a string, here appended to StringBuilder
     * @param shasum the given byte-array
     * @param sb optional pre-existing StringBuilder, may be null
     * @return return given or new StringBuilder with appended hex-string
     */
    public static StringBuilder toHexString(final byte[] shasum, StringBuilder sb) {
        if( null == sb ) {
            sb = new StringBuilder();
        }
        for(int i=0; i<shasum.length; i++) {
            sb.append(String.format((Locale)null, "%02x", shasum[i]));
        }
        return sb;
    }

    /**
     * Returns the sorted list of given strings using {@link String#compareTo(String)}'s lexicographically comparison.
     * @param source given input strings
     * @return sorted list of given strings
     */
    public static List<String> sort(final ArrayList<String> source) {
        final String s[] = source.toArray(new String[source.size()]);
        Arrays.sort(s, 0, s.length, null);
        return Arrays.asList(s);
    }

    final MessageDigest digest;
    final List<String> origins;
    final List<Pattern> excludes, includes;

    /**
     * Instance to ensure proper {@link #compute(boolean)} of identical SHA sums over same contents within given paths across machines.
     * <p>
     * Instantiation of this class is lightweight, {@link #compute(boolean)} performs all operations.
     * </p>
     *
     * @param digest the SHA algorithm
     * @param origins the mandatory path origins to be used for {@link IOUtil#filesOf(List, List, List)}
     * @param excludes the optional exclude patterns to be used for {@link IOUtil#filesOf(List, List, List)}
     * @param includes the optional include patterns to be used for {@link IOUtil#filesOf(List, List, List)}
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws URISyntaxException
     */
    public SHASum(final MessageDigest digest, final List<String> origins, final List<Pattern> excludes, final List<Pattern> includes) {
        this.digest = digest;
        this.origins = origins;
        this.excludes = excludes;
        this.includes = includes;
    }

    /**
     * Implementation gathers all files traversing through given paths via {@link IOUtil#filesOf(List, List, List)},
     * sorts the resulting file list via {@link #sort(ArrayList)} and finally
     * calculates the SHA sum over its byte content via {@link #updateDigest(MessageDigest, List)}.
     * <p>
     * This ensures identical SHA sums over same contents within given paths across machines.
     * </p>
     * <p>
     * This method is heavyweight and performs all operations.
     * </p>
     *
     * @param verbose if true, all used files will be dumped as well as the digest result
     * @return the resulting SHA value
     * @throws IOException
     */
    public final byte[] compute(final boolean verbose) throws IOException {
        final List<String> fnamesS = SHASum.sort(IOUtil.filesOf(origins, excludes, includes));
        if( verbose ) {
            for(int i=0; i<fnamesS.size(); i++) {
                System.err.println(fnamesS.get(i));
            }
        }
        final long numBytes = SHASum.updateDigest(digest, fnamesS);
        final byte[] shasum = digest.digest();
        if( verbose ) {
            System.err.println("Digested "+numBytes+" bytes, shasum size "+shasum.length+" bytes");
            System.err.println("Digested result: "+SHASum.toHexString(shasum, null).toString());
        }
        return shasum;
    }

    public final List<String> getOrigins() { return origins; }
    public final List<Pattern> getExcludes() { return excludes; }
    public final List<Pattern> getIncludes() { return includes; }

    /**
     * Main entry point taking var-arg path or gnu-arguments with a leading '--'.
     * <p>
     * Implementation gathers all files traversing through given paths via {@link IOUtil#filesOf(List, List, List)},
     * sorts the resulting file list via {@link #sort(ArrayList)} and finally
     * calculates the SHA sum over its byte content via {@link #updateDigest(MessageDigest, List)}.
     * This ensures identical SHA sums over same contents within given paths.
     * </p>
     * <p>
     * Example to calculate the SHA-256 over our source files as performed for {@link JauVersion#getImplementationSHASources()}
     * <pre>
     * java -cp build/gluegen-rt.jar com.jogamp.common.util.SHASum --algorithm 256 --exclude ".*\\.log" --exclude "make/lib/toolchain" src jcpp/src make
     * </pre>
     * </p>
     * <p>
     * To validate the implementation, one can gather the sorted list of files (to ensure same order)
     * <pre>
     * java -cp build/gluegen-rt.jar com.jogamp.common.util.SHASum --listfilesonly --exclude ".*\\.log" --exclude "make/lib/toolchain" src jcpp/src make >& java.sorted.txt
     * </pre>
     * and then calculate the shasum independently
     * <pre>
     * find `cat java.sorted.txt` -exec cat {} + | shasum -a 256 -b - | awk '{print $1}'
     * </pre>
     * </p>
     * @param args
     * @throws IOException
     * @throws URISyntaxException
     * @throws IllegalArgumentException
     */
    public static void main(final String[] args) throws IOException {
        boolean listFilesOnly = false;
        int shabits = 256;
        int i;
        final ArrayList<String> pathU = new ArrayList<String>();
        final ArrayList<Pattern> excludes = new ArrayList<Pattern>();
        final ArrayList<Pattern> includes = new ArrayList<Pattern>();
        {
            for(i=0; i<args.length; i++) {
                if(null != args[i]) {
                    if( args[i].startsWith("--") ) {
                        // options
                        if( args[i].equals("--algorithm")) {
                            shabits = Integer.parseInt(args[++i]);
                        } else if( args[i].equals("--exclude")) {
                            excludes.add(Pattern.compile(args[++i]));
                            if( DEBUG ) {
                                System.err.println("adding exclude: <"+args[i]+"> -> <"+excludes.get(excludes.size()-1)+">");
                            }
                        } else if( args[i].equals("--include")) {
                            includes.add(Pattern.compile(args[++i]));
                            if( DEBUG ) {
                                System.err.println("adding include: <"+args[i]+"> -> <"+includes.get(includes.size()-1)+">");
                            }
                        } else if( args[i].equals("--listfilesonly")) {
                            listFilesOnly = true;
                        } else {
                            System.err.println("Abort, unknown argument: "+args[i]);
                            return;
                        }
                    } else {
                        pathU.add(args[i]);
                        if( DEBUG ) {
                            System.err.println("adding path: <"+args[i]+">");
                        }
                    }
                }
            }
            if( listFilesOnly ) {
                final List<String> fnamesS = sort(IOUtil.filesOf(pathU, excludes, includes));
                for(i=0; i<fnamesS.size(); i++) {
                    System.out.println(fnamesS.get(i));
                }
                return;
            }
        }
        final String shaalgo = "SHA-"+shabits;
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(shaalgo);
        } catch (final NoSuchAlgorithmException e) {
            System.err.println("Abort, implementation for "+shaalgo+" not available: "+e.getMessage());
            return;
        }
        final SHASum shaSum = new SHASum(digest, pathU, excludes, includes);
        System.out.println(toHexString(shaSum.compute(DEBUG), null).toString());
    }
}
