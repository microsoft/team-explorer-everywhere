// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * <p>
 * {@link HashUtils} contains static utility methods to create hashes of input
 * data. {@link HashUtils} uses the {@link MessageDigest} class to perform
 * hashing.
 * </p>
 *
 * <p>
 * {@link HashUtils} has the following advantages:
 * <ul>
 * <li>Common checked exceptions such as {@link NoSuchAlgorithmException} that
 * normally indicate programming errors are converted to unchecked exceptions
 * that client code is not forced to deal with</li>
 * <li>Hashing of different types of data ({@link String}s, <code>byte</code>
 * arrays, {@link File}s, and {@link InputStream}s) is supported</li>
 * <li>For stream-based hashing, cancelation is supported through use of
 * {@link TaskMonitor} and {@link TaskMonitorService}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Note that {@link MessageDigest} can support a number of hash functions.
 * {@link HashUtils} provides constants for standard hash functions that can be
 * assumed to be present. For more information, see:
 * <ul>
 * <li>http://java.sun.com/javase/6/docs/technotes/guides/security/
 * StandardNames. html#MessageDigest</li>
 * <li>http://java.sun.com/javase/6/docs/technotes/guides/security/SunProviders.
 * html</li>
 * </ul>
 * </p>
 *
 * <p>
 * To compare two hash values, use {@link Arrays#equals(byte[], byte[])}. To
 * convert a hash value into a {@link String} of hex digits, use
 * {@link ArrayUtils#byteArrayToHexString(byte[])}.
 * </p>
 */
public class HashUtils {
    /**
     * The name of the <code>MD2</code> hash algorithm, as defined in RFC 1319.
     */
    public static final String ALGORITHM_MD2 = "MD2"; //$NON-NLS-1$

    /**
     * The name of the <code>MD5</code> hash algorithm, as defined in RFC 1321.
     */
    public static final String ALGORITHM_MD5 = "MD5"; //$NON-NLS-1$

    /**
     * The name of the <code>SHA-1</code> hash algorithm, as defined in FIPS PUB
     * 180-2.
     */
    public static final String ALGORITHM_SHA_1 = "SHA-1"; //$NON-NLS-1$

    /**
     * The name of the <code>SHA-256</code> hash algorithm, as defined in FIPS
     * PUB 180-2.
     */
    public static final String ALGORITHM_SHA_256 = "SHA-256"; //$NON-NLS-1$

    /**
     * The name of the <code>SHA-384</code> hash algorithm, as defined in FIPS
     * PUB 180-2.
     */
    public static final String ALGORITHM_SHA_384 = "SHA-384"; //$NON-NLS-1$

    /**
     * The name of the <code>SHA-512</code> hash algorithm, as defined in FIPS
     * PUB 180-2.
     */
    public static final String ALGORITHM_SHA_512 = "SHA-512"; //$NON-NLS-1$

    /**
     * Tests whether a given hash algorithm is available from the registered
     * providers.
     *
     * @param algorithm
     *        the algorithm name to test (must not be <code>null</code>)
     * @return <code>true</code> if the given algorithm is available
     */
    public static boolean isAlgorithmAvailable(final String algorithm) {
        Check.notNull(algorithm, "algorithm"); //$NON-NLS-1$

        try {
            MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            return false;
        }

        return true;
    }

    /**
     * Tests whether a given provider is available.
     *
     * @param providerName
     *        the provider name to test (must not be <code>null</code>)
     * @return <code>true</code> if the given provider is available
     */
    public static boolean isProviderAvailable(final String providerName) {
        Check.notNull(providerName, "providerName"); //$NON-NLS-1$

        return Security.getProvider(providerName) != null;
    }

    /**
     * <p>
     * Hashes the given {@link String}. The {@link String} is first converted to
     * a <code>byte</code> array by converting the characters to bytes using the
     * specified charset (or the default charset if <code>charsetName</code> is
     * <code>null</code>).
     * </p>
     *
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws UncheckedUnsupportedEncodingException
     *         if the given charset is not supported
     *
     * @param input
     *        the {@link String} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @return the hash of the given {@link String} using the given algorithm
     */
    public static byte[] hashString(final String input, final String charsetName, final String algorithm)
        throws UncheckedNoSuchAlgorithmException,
            UncheckedUnsupportedEncodingException {
        final MessageDigest digester = getMessageDigestInstance(algorithm);
        return hashString(input, charsetName, digester);
    }

    /**
     * <p>
     * Hashes the given {@link String}. The {@link String} is first converted to
     * a <code>byte</code> array by converting the characters to bytes using the
     * specified charset (or the default charset if <code>charsetName</code> is
     * <code>null</code>).
     * </p>
     *
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws UncheckedNoSuchProviderException
     *         if the given provider name is not registered
     * @throws UncheckedUnsupportedEncodingException
     *         if the given charset is not supported
     *
     * @param input
     *        the {@link String} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param providerName
     *        the name of the provider of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @return the hash of the given {@link String} using the given algorithm
     *         from the given provider
     */
    public static byte[] hashString(
        final String input,
        final String charsetName,
        final String algorithm,
        final String providerName)
            throws UncheckedNoSuchAlgorithmException,
                UncheckedNoSuchProviderException,
                UncheckedUnsupportedEncodingException {
        final MessageDigest digester = getMessageDigestInstance(algorithm, providerName);
        return hashString(input, charsetName, digester);
    }

    /**
     * <p>
     * Hashes the given {@link String}. The {@link String} is first converted to
     * a <code>byte</code> array by converting the characters to bytes using the
     * specified charset (or the default charset if <code>charsetName</code> is
     * <code>null</code>).
     * </p>
     *
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws UncheckedUnsupportedEncodingException
     *         if the given charset is not supported
     *
     * @param input
     *        the {@link String} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param providerName
     *        the name of the provider of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @return the hash of the given {@link String} using the given algorithm
     *         from the given {@link Provider}
     */
    public static byte[] hashString(
        final String input,
        final String charsetName,
        final String algorithm,
        final Provider provider) throws UncheckedNoSuchAlgorithmException, UncheckedUnsupportedEncodingException {
        final MessageDigest digester = getMessageDigestInstance(algorithm, provider);
        return hashString(input, charsetName, digester);
    }

    private static byte[] hashString(final String input, final String charsetName, final MessageDigest digester)
        throws UncheckedUnsupportedEncodingException {
        Check.notNull(input, "input"); //$NON-NLS-1$

        byte[] bytes;
        if (charsetName == null) {
            bytes = input.getBytes();
        } else {
            try {
                bytes = input.getBytes(charsetName);
            } catch (final UnsupportedEncodingException e) {
                throw new UncheckedUnsupportedEncodingException(charsetName, e);
            }
        }

        return hashBytes(bytes, digester);
    }

    /**
     * <p>
     * Hashes the given <code>byte</code> array.
     * </p>
     *
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     *
     * @param input
     *        the <code>byte</code> array to hash (must not be <code>null</code>
     *        )
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @return the hash of the given <code>byte</code> array using the given
     *         algorithm
     */
    public static byte[] hashBytes(final byte[] input, final String algorithm)
        throws UncheckedNoSuchAlgorithmException {
        final MessageDigest digester = getMessageDigestInstance(algorithm);
        return hashBytes(input, digester);
    }

    /**
     * <p>
     * Hashes the given <code>byte</code> array.
     * </p>
     *
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws UncheckedNoSuchProviderException
     *         if the given provider name is not registered
     *
     * @param input
     *        the <code>byte</code> array to hash (must not be <code>null</code>
     *        )
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param providerName
     *        the name of the provider of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @return the hash of the given <code>byte</code> array using the given
     *         algorithm from the given provider
     */
    public static byte[] hashBytes(final byte[] input, final String algorithm, final String providerName)
        throws UncheckedNoSuchAlgorithmException,
            UncheckedNoSuchProviderException {
        final MessageDigest digester = getMessageDigestInstance(algorithm, providerName);
        return hashBytes(input, digester);
    }

    /**
     * <p>
     * Hashes the given <code>byte</code> array.
     * </p>
     *
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     *
     * @param input
     *        the <code>byte</code> array to hash (must not be <code>null</code>
     *        )
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param provider
     *        the {@link Provider} of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @return the hash of the given <code>byte</code> array using the given
     *         algorithm from the given {@link Provider}
     */
    public static byte[] hashBytes(final byte[] input, final String algorithm, final Provider provider) {
        final MessageDigest digester = getMessageDigestInstance(algorithm, provider);
        return hashBytes(input, digester);
    }

    private static byte[] hashBytes(final byte[] input, final MessageDigest digester) {
        Check.notNull(input, "input"); //$NON-NLS-1$

        return digester.digest(input);
    }

    /**
     * @equivalence hashFile(file, algorithm, (TaskMonitor) null)
     */
    public static byte[] hashFile(final File file, final String algorithm)
        throws FileNotFoundException,
            FileNotFoundException,
            IOException,
            UncheckedNoSuchAlgorithmException {
        return hashFile(file, algorithm, (TaskMonitor) null);
    }

    /**
     * <p>
     * Hashes the contents of the given {@link File}.
     * </p>
     *
     * @throws FileNotFoundException
     *         if the given file does not exist or can't be opened
     * @throws IOException
     *         if an exception is encountered reading the file
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation
     *
     * @param file
     *        the {@link File} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to detect cancelation (if
     *        <code>null</code> no cancelation detection is performed)
     * @return the hash of the given file using the given algorithm
     */
    public static byte[] hashFile(final File file, final String algorithm, final TaskMonitor taskMonitor)
        throws FileNotFoundException,
            CanceledException,
            FileNotFoundException,
            IOException,
            UncheckedNoSuchAlgorithmException {
        final MessageDigest digester = getMessageDigestInstance(algorithm);
        return hashFile(file, digester, taskMonitor);
    }

    /**
     * @equivalence hashFile(file, algorithm, providerName, (TaskMonitor) null)
     */
    public static byte[] hashFile(final File file, final String algorithm, final String providerName)
        throws FileNotFoundException,
            IOException,
            UncheckedNoSuchAlgorithmException,
            UncheckedNoSuchProviderException {
        return hashFile(file, algorithm, providerName, (TaskMonitor) null);
    }

    /**
     * <p>
     * Hashes the contents of the given {@link File}.
     * </p>
     *
     * @throws FileNotFoundException
     *         if the given file does not exist or can't be opened
     * @throws IOException
     *         if an exception is encountered reading the file
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws UncheckedNoSuchProviderException
     *         if the given provider name is not registered
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation
     *
     * @param file
     *        the {@link File} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param providerName
     *        the name of the provider of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to detect cancelation (if
     *        <code>null</code> no cancelation detection is performed)
     * @return the hash of the given file using the given algorithm from the
     *         given provider
     */
    public static byte[] hashFile(
        final File file,
        final String algorithm,
        final String providerName,
        final TaskMonitor taskMonitor)
            throws FileNotFoundException,
                IOException,
                UncheckedNoSuchAlgorithmException,
                UncheckedNoSuchProviderException,
                CanceledException {
        final MessageDigest digester = getMessageDigestInstance(algorithm, providerName);
        return hashFile(file, digester, taskMonitor);
    }

    /**
     * @equivalence hashFile(file, algorithm, provider, (TaskMonitor) null)
     */
    public static byte[] hashFile(final File file, final String algorithm, final Provider provider)
        throws FileNotFoundException,
            IOException,
            UncheckedNoSuchAlgorithmException {
        return hashFile(file, algorithm, provider, (TaskMonitor) null);
    }

    /**
     * <p>
     * Hashes the contents of the given {@link File}.
     * </p>
     *
     * @throws FileNotFoundException
     *         if the given file does not exist or can't be opened
     * @throws IOException
     *         if an exception is encountered reading the file
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation
     *
     * @param file
     *        the {@link File} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param providerName
     *        the name of the provider of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @param provider
     *        the {@link Provider} of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to detect cancelation (if
     *        <code>null</code> no cancelation detection is performed)
     * @return the hash of the given file using the given algorithm from the
     *         given {@link Provider}
     */
    public static byte[] hashFile(
        final File file,
        final String algorithm,
        final Provider provider,
        final TaskMonitor taskMonitor)
            throws FileNotFoundException,
                IOException,
                UncheckedNoSuchAlgorithmException,
                CanceledException {
        final MessageDigest digester = getMessageDigestInstance(algorithm, provider);
        return hashFile(file, digester, taskMonitor);
    }

    /**
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation (
     *         <code>null</code> monitor never will)
     */
    private static byte[] hashFile(final File file, final MessageDigest digester, final TaskMonitor taskMonitor)
        throws CanceledException,
            FileNotFoundException,
            IOException {
        Check.notNull(file, "file"); //$NON-NLS-1$

        final FileInputStream stream = new FileInputStream(file);
        return hashStream(stream, digester, taskMonitor);
    }

    /**
     * @equivalence hashStream(stream, algorithm, (TaskMonitor) null)
     */
    public static byte[] hashStream(final InputStream stream, final String algorithm)
        throws IOException,
            UncheckedNoSuchAlgorithmException {
        return hashStream(stream, algorithm, (TaskMonitor) null);
    }

    /**
     * <p>
     * Hashes the given {@link InputStream}. All bytes are read and hashed until
     * end of stream is reached. The stream is always closed by this method,
     * even if an exception is thrown.
     * </p>
     *
     * @throws IOException
     *         if an exception is encountered reading the stream
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation
     *
     * @param stream
     *        the {@link InputStream} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to detect cancelation (if
     *        <code>null</code> no cancelation detection is performed)
     * @return the hash of the given stream using the given algorithm
     */
    public static byte[] hashStream(final InputStream stream, final String algorithm, final TaskMonitor taskMonitor)
        throws IOException,
            UncheckedNoSuchAlgorithmException,
            CanceledException {
        final MessageDigest digester = getMessageDigestInstance(algorithm);
        return hashStream(stream, digester, taskMonitor);
    }

    /**
     * @equivalence hashStream(stream, algorithm, providerName, (TaskMonitor)
     *              null)
     */
    public static byte[] hashStream(final InputStream stream, final String algorithm, final String providerName)
        throws IOException,
            UncheckedNoSuchAlgorithmException,
            UncheckedNoSuchProviderException {
        return hashStream(stream, algorithm, providerName, (TaskMonitor) null);
    }

    /**
     * <p>
     * Hashes the given {@link InputStream}. All bytes are read and hashed until
     * end of stream is reached. The stream is always closed by this method,
     * even if an exception is thrown.
     * </p>
     *
     * @throws IOException
     *         if an exception is encountered reading the stream
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws UncheckedNoSuchProviderException
     *         if the given provider name is not registered
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation
     *
     * @param stream
     *        the {@link InputStream} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param providerName
     *        the name of the provider of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to detect cancelation (if
     *        <code>null</code> no cancelation detection is performed)
     * @return the hash of the given stream using the given algorithm from the
     *         given provider
     */
    public static byte[] hashStream(
        final InputStream stream,
        final String algorithm,
        final String providerName,
        final TaskMonitor taskMonitor)
            throws IOException,
                UncheckedNoSuchAlgorithmException,
                UncheckedNoSuchProviderException,
                CanceledException {
        final MessageDigest digester = getMessageDigestInstance(algorithm, providerName);
        return hashStream(stream, digester, taskMonitor);
    }

    /**
     * @equivalence hashStream(stream, algorithm, provider, (TaskMonitor) null)
     */
    public static byte[] hashStream(final InputStream stream, final String algorithm, final Provider provider)
        throws IOException,
            UncheckedNoSuchAlgorithmException {
        return hashStream(stream, algorithm, provider, (TaskMonitor) null);
    }

    /**
     * <p>
     * Hashes the given {@link InputStream}. All bytes are read and hashed until
     * end of stream is reached. The stream is always closed by this method,
     * even if an exception is thrown.
     * </p>
     *
     * @throws IOException
     *         if an exception is encountered reading the stream
     * @throws UncheckedNoSuchAlgorithmException
     *         if the given algorithm is not supported
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation
     *
     * @param stream
     *        the {@link InputStream} to hash (must not be <code>null</code>)
     * @param algorithm
     *        the hash algorithm to use (must not be <code>null</code>)
     * @param provider
     *        the {@link Provider} of the hash algorithm to use (must not be
     *        <code>null</code>)
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to detect cancelation (if
     *        <code>null</code> no cancelation detection is performed)
     * @return the hash of the given stream using the given algorithm from the
     *         given {@link Provider}
     */
    public static byte[] hashStream(
        final InputStream stream,
        final String algorithm,
        final Provider provider,
        final TaskMonitor taskMonitor) throws IOException, UncheckedNoSuchAlgorithmException, CanceledException {
        final MessageDigest digester = getMessageDigestInstance(algorithm, provider);
        return hashStream(stream, digester, taskMonitor);
    }

    /**
     * @throws CanceledException
     *         if the given {@link TaskMonitor} signals cancelation (
     *         <code>null</code> monitor never will)
     */
    private static byte[] hashStream(
        final InputStream stream,
        final MessageDigest digester,
        final TaskMonitor taskMonitor) throws IOException, CanceledException {
        Check.notNull(stream, "stream"); //$NON-NLS-1$

        final byte[] buffer = new byte[65536];
        int read = 0;

        try {
            if (taskMonitor != null && taskMonitor.isCanceled()) {
                throw new CanceledException();
            }

            while ((read = stream.read(buffer)) != -1) {
                if (taskMonitor != null && taskMonitor.isCanceled()) {
                    throw new CanceledException();
                }

                digester.update(buffer, 0, read);
            }

            return digester.digest();
        } finally {
            IOUtils.closeSafely(stream);
        }
    }

    /**
     * An unchecked version of {@link UnsupportedEncodingException}. This is a
     * {@link RuntimeException} whose cause is always a
     * {@link UnsupportedEncodingException}.
     */
    public static class UncheckedUnsupportedEncodingException extends RuntimeException {
        /**
         * Creates a new {@link UncheckedUnsupportedEncodingException}.
         *
         * @param encoding
         *        the encoding name that caused the
         *        {@link UnsupportedEncodingException} to be thrown
         * @param cause
         *        the checked {@link UnsupportedEncodingException}
         */
        public UncheckedUnsupportedEncodingException(final String encoding, final UnsupportedEncodingException cause) {
            super("unsupported encoding: [" + encoding + "]", cause); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * An unchecked version of {@link NoSuchAlgorithmException}. This is a
     * {@link RuntimeException} whose cause is always a
     * {@link NoSuchAlgorithmException}.
     */
    public static class UncheckedNoSuchAlgorithmException extends RuntimeException {
        /**
         * Creates a new {@link UncheckedNoSuchAlgorithmException}.
         *
         * @param algorithm
         *        the algorithm name that caused the
         *        {@link NoSuchAlgorithmException} to be thrown
         * @param cause
         *        the checked {@link NoSuchAlgorithmException}
         */
        public UncheckedNoSuchAlgorithmException(final String algorithm, final NoSuchAlgorithmException cause) {
            super("no such algorithm: [" + algorithm + "]", cause); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * An unchecked version of {@link NoSuchProviderException}. This is a
     * {@link RuntimeException} whose cause is always a
     * {@link NoSuchProviderException}.
     */
    public static class UncheckedNoSuchProviderException extends RuntimeException {
        /**
         * Creates a new {@link UncheckedNoSuchProviderException}.
         *
         * @param providerName
         *        the provider name that caused the
         *        {@link NoSuchProviderException} to be thrown
         * @param cause
         *        the checked {@link NoSuchProviderException}
         */
        public UncheckedNoSuchProviderException(final String providerName, final NoSuchProviderException cause) {
            super("no such provider: [" + providerName + "]", cause); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static MessageDigest getMessageDigestInstance(final String algorithm, final Provider provider)
        throws UncheckedNoSuchAlgorithmException {
        Check.notNull(algorithm, "algorithm"); //$NON-NLS-1$
        Check.notNull(provider, "provider"); //$NON-NLS-1$

        try {
            return MessageDigest.getInstance(algorithm, provider);
        } catch (final NoSuchAlgorithmException e) {
            throw new UncheckedNoSuchAlgorithmException(algorithm, e);
        }
    }

    private static MessageDigest getMessageDigestInstance(final String algorithm, final String providerName)
        throws UncheckedNoSuchAlgorithmException,
            UncheckedNoSuchProviderException {
        Check.notNull(algorithm, "algorithm"); //$NON-NLS-1$
        Check.notNull(providerName, "providerName"); //$NON-NLS-1$

        try {
            return MessageDigest.getInstance(algorithm, providerName);
        } catch (final NoSuchAlgorithmException e) {
            throw new UncheckedNoSuchAlgorithmException(algorithm, e);
        } catch (final NoSuchProviderException e) {
            throw new UncheckedNoSuchProviderException(providerName, e);
        }
    }

    private static MessageDigest getMessageDigestInstance(final String algorithm)
        throws UncheckedNoSuchAlgorithmException {
        Check.notNull(algorithm, "algorithm"); //$NON-NLS-1$

        try {
            return MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new UncheckedNoSuchAlgorithmException(algorithm, e);
        }
    }
}
