// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

/**
 *
 *
 * @threadsafety unknown
 */
public class DotNETDateTest extends TestCase {
    /**
     * Test that we correctly deserialize binary values of DateTime objects into
     * the representative {@link Calendar} representation.
     * <p>
     * We do not test any dates before the Gregorian reformation (October 15,
     * 1582) as there are inconsistencies in handling dates before this between
     * .NET and Java. See {@link #testRoundTrip()} below.
     */
    public void testBinaryDeserialization() {
        /*
         * Test a given date (05/13/2011 10:21:42.142), ensuring that when the
         * binary format is the "unspecified" time zone is treated like a UTC
         * format.
         */
        assertEquals(
            DotNETDate.fromBinary(0x8cddf6eb001e9e0L).getTime(),
            getCalendar(2011, 04, 13, 10, 21, 42, 142).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x48cddf6eb001e9e0L).getTime(),
            getCalendar(2011, 04, 13, 10, 21, 42, 142).getTime());

        /*
         * Test some random dates (post October 15, 1582.)
         */
        assertEquals(
            DotNETDate.fromBinary(0x8af166bb80538d0L).getTime(),
            getCalendar(1983, 10, 27, 4, 35, 13, 245).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x836a9d752b2ef00L).getTime(),
            getCalendar(1876, 5, 28, 6, 25, 31, 632).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xb447d709fa7b7f0L).getTime(),
            getCalendar(2573, 10, 5, 16, 19, 10, 191).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xb06aa2e95341ff0L).getTime(),
            getCalendar(2518, 8, 14, 4, 22, 12, 591).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xbcf3a9281af0400L).getTime(),
            getCalendar(2697, 7, 6, 4, 12, 31, 168).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xb70a304e4ed3bb0L).getTime(),
            getCalendar(2613, 2, 23, 21, 2, 2, 475).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x90f5a897b3b8950L).getTime(),
            getCalendar(2069, 9, 7, 20, 55, 51, 653).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x9a8c7f45edcd4e0L).getTime(),
            getCalendar(2206, 7, 15, 15, 24, 40, 622).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x7f230178fc97540L).getTime(),
            getCalendar(1815, 5, 1, 5, 0, 38, 420).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xb93e504af70aa20L).getTime(),
            getCalendar(2644, 8, 3, 5, 0, 27, 970).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xb91567b35c288c0L).getTime(),
            getCalendar(2642, 4, 24, 6, 8, 42, 316).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xcaba29a2e05c210L).getTime(),
            getCalendar(2894, 2, 10, 10, 20, 55, 345).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x6f62c830476c720L).getTime(),
            getCalendar(1590, 7, 17, 18, 49, 32, 562).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xb879da716830390L).getTime(),
            getCalendar(2633, 8, 21, 0, 22, 3, 593).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x77e668d654b8c30L).getTime(),
            getCalendar(1712, 1, 19, 21, 24, 40, 691).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xcdbaddb9fbaee40L).getTime(),
            getCalendar(2937, 0, 16, 6, 5, 14, 404).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xbff2d62c0bfe130L).getTime(),
            getCalendar(2740, 4, 13, 21, 26, 4, 227).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x9e71eb38ccd7f90L).getTime(),
            getCalendar(2262, 2, 23, 11, 17, 27, 177).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xbca376445df9e20L).getTime(),
            getCalendar(2693, 1, 15, 5, 19, 12, 130).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x7f29c672982d940L).getTime(),
            getCalendar(1815, 9, 17, 1, 2, 32, 148).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x7c8167688e137c0L).getTime(),
            getCalendar(1777, 10, 10, 18, 46, 36, 604).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x8eb857a7b2e64d0L).getTime(),
            getCalendar(2037, 9, 22, 9, 28, 12, 957).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x9e2cc052a21ca40L).getTime(),
            getCalendar(2258, 4, 15, 3, 2, 43, 172).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xce22c30ad226bb0L).getTime(),
            getCalendar(2942, 10, 1, 17, 1, 16, 651).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x9f3007ce1a0fe50L).getTime(),
            getCalendar(2272, 9, 27, 9, 30, 4, 341).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x86c5c13e6cec960L).getTime(),
            getCalendar(1924, 4, 21, 11, 50, 33, 462).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x87fcf29a48b2cf0L).getTime(),
            getCalendar(1941, 8, 25, 19, 0, 10, 687).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x9e03971a419cba0L).getTime(),
            getCalendar(2256, 0, 28, 0, 48, 57, 178).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xd08c1bff9114000L).getTime(),
            getCalendar(2977, 3, 1, 17, 20, 56, 832).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xd1d7f947dee9fb0L).getTime(),
            getCalendar(2995, 9, 1, 22, 7, 36, 235).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xbb56a4a1e499920L).getTime(),
            getCalendar(2674, 6, 28, 14, 6, 17, 10).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x7c2c34553579010L).getTime(),
            getCalendar(1773, 1, 10, 0, 10, 26, 577).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x8ef4402494fcc20L).getTime(),
            getCalendar(2041, 1, 23, 4, 53, 54, 786).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xbabe2618102d100L).getTime(),
            getCalendar(2666, 0, 26, 14, 26, 23, 888).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xc9c4510f1fa8d00L).getTime(),
            getCalendar(2880, 5, 25, 16, 19, 50, 352).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xacc2be575852b60L).getTime(),
            getCalendar(2466, 6, 12, 3, 53, 34, 742).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x976cc06eb5b2d20L).getTime(),
            getCalendar(2162, 0, 13, 18, 19, 28, 370).getTime());
        assertEquals(
            DotNETDate.fromBinary(0x83ec1be0e7611d0L).getTime(),
            getCalendar(1883, 8, 16, 22, 24, 49, 5).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xa44039ec6e47120L).getTime(),
            getCalendar(2345, 0, 30, 15, 52, 7, 218).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xabd7eb2be255730L).getTime(),
            getCalendar(2453, 5, 8, 19, 35, 13, 699).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xa93e65db0844030L).getTime(),
            getCalendar(2416, 4, 2, 22, 20, 2, 355).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xcfdb52baa393bf0L).getTime(),
            getCalendar(2967, 4, 25, 2, 54, 30, 703).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xb84e876c7eac250L).getTime(),
            getCalendar(2631, 3, 22, 21, 0, 20, 85).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xa0c47484a33e6a0L).getTime(),
            getCalendar(2295, 4, 15, 0, 26, 30, 794).getTime());
        assertEquals(
            DotNETDate.fromBinary(0xaa6c2248f8caa60L).getTime(),
            getCalendar(2433, 1, 26, 16, 14, 39, 622).getTime());
    }

    /**
     * Test that we correctly serialize {@link Calendar} objects into the
     * representative binary values of DateTime objects.
     * <p>
     * We do not test any dates before the Gregorian reformation (October 15,
     * 1582) as there are inconsistencies in handling dates before this between
     * .NET and Java. See {@link #testRoundTrip()} below.
     */
    public void testBinarySerialization() {
        assertEquals(DotNETDate.toBinary(getCalendar(2011, 04, 13, 10, 21, 42, 142)), 0x48cddf6eb001e9e0L);
        assertEquals(DotNETDate.toBinary(getCalendar(1955, 10, 14, 16, 42, 11, 857)), 0x488fa830c0bd3810L);

        assertEquals(DotNETDate.toBinary(getCalendar(2551, 6, 8, 0, 28, 51, 624)), 0x4b2b73e89ccfda80L);
        assertEquals(DotNETDate.toBinary(getCalendar(2156, 10, 11, 0, 7, 52, 156)), 0x4970ff0c764049c0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2126, 5, 10, 2, 55, 32, 269)), 0x494ee34ae455b5d0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2508, 7, 28, 5, 39, 16, 545)), 0x4afb671c7baec310L);
        assertEquals(DotNETDate.toBinary(getCalendar(2399, 3, 13, 17, 1, 38, 536)), 0x4a80c76c36a40e80L);
        assertEquals(DotNETDate.toBinary(getCalendar(2138, 5, 27, 14, 43, 58, 813)), 0x495c6536d5c998d0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2772, 2, 1, 17, 41, 48, 724)), 0x4c22d45ec0a34f40L);
        assertEquals(DotNETDate.toBinary(getCalendar(1957, 3, 26, 21, 5, 22, 30)), 0x4891480628f028e0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2780, 3, 23, 18, 34, 4, 827)), 0x4c2bf629f0d4e6b0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2476, 10, 9, 2, 14, 36, 990)), 0x4ad7c0adbaeadde0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2988, 4, 15, 4, 32, 26, 812)), 0x4d153943974f0fc0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2228, 9, 15, 22, 27, 48, 311)), 0x49c1a2d5f5dc7e70L);
        assertEquals(DotNETDate.toBinary(getCalendar(1642, 6, 24, 21, 16, 29, 545)), 0x4730667f419f5d90L);
        assertEquals(DotNETDate.toBinary(getCalendar(1805, 1, 22, 3, 54, 58, 208)), 0x47e6ac81efc09200L);
        assertEquals(DotNETDate.toBinary(getCalendar(2084, 7, 20, 10, 11, 43, 272)), 0x492005e24878da80L);
        assertEquals(DotNETDate.toBinary(getCalendar(2068, 0, 26, 2, 32, 38, 188)), 0x490d72bca4bd56c0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2536, 6, 9, 19, 3, 21, 256)), 0x4b1aa4abe5bf3280L);
        assertEquals(DotNETDate.toBinary(getCalendar(2956, 8, 10, 8, 7, 30, 554)), 0x4cf1b5a2c32255a0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2681, 10, 26, 20, 45, 31, 99)), 0x4bbda2e3924802b0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2492, 6, 8, 9, 5, 48, 79)), 0x4ae94fb2d6e24bf0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2330, 3, 25, 20, 49, 37, 256)), 0x4a337528ae289680L);
        assertEquals(DotNETDate.toBinary(getCalendar(1850, 8, 3, 17, 58, 11, 236)), 0x4819b817c2c0be40L);
        assertEquals(DotNETDate.toBinary(getCalendar(2647, 9, 12, 10, 15, 49, 566)), 0x4b97604a9e0795e0L);
        assertEquals(DotNETDate.toBinary(getCalendar(1800, 10, 7, 17, 7, 5, 31)), 0x47e1dcccd03a3d70L);
        assertEquals(DotNETDate.toBinary(getCalendar(1599, 0, 13, 22, 31, 10, 441)), 0x46ff99d5c5345590L);
        assertEquals(DotNETDate.toBinary(getCalendar(2307, 4, 16, 6, 52, 17, 585)), 0x4a19bbab57522a10L);
        assertEquals(DotNETDate.toBinary(getCalendar(1761, 8, 4, 17, 20, 50, 889)), 0x47b5f1883e0b2b90L);
        assertEquals(DotNETDate.toBinary(getCalendar(2627, 9, 29, 19, 57, 30, 373)), 0x4b8101ac6df61b50L);
        assertEquals(DotNETDate.toBinary(getCalendar(2038, 7, 3, 16, 32, 13, 177)), 0x48ec65a9ee76a690L);
        assertEquals(DotNETDate.toBinary(getCalendar(2651, 7, 25, 10, 0, 46, 240)), 0x4b9bb69f9d4bba00L);
        assertEquals(DotNETDate.toBinary(getCalendar(2809, 9, 13, 1, 10, 47, 972)), 0x4c4d00c4b3119640L);
        assertEquals(DotNETDate.toBinary(getCalendar(2503, 1, 4, 1, 29, 52, 601)), 0x4af52a38fd161c90L);
        assertEquals(DotNETDate.toBinary(getCalendar(1798, 1, 11, 16, 31, 58, 758)), 0x47decbc3661e3c60L);
        assertEquals(DotNETDate.toBinary(getCalendar(2473, 10, 29, 2, 52, 7, 645)), 0x4ad4732cb3ee90d0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2048, 5, 13, 5, 46, 33, 700)), 0x48f773c67fdf1240L);
        assertEquals(DotNETDate.toBinary(getCalendar(1786, 0, 4, 4, 13, 1, 827)), 0x47d13952b6feb530L);
        assertEquals(DotNETDate.toBinary(getCalendar(2972, 8, 28, 20, 46, 28, 838)), 0x4d03b46dfb938860L);
        assertEquals(DotNETDate.toBinary(getCalendar(2375, 6, 12, 19, 7, 50, 237)), 0x4a6625dc6fcaf0d0L);
        assertEquals(DotNETDate.toBinary(getCalendar(2941, 4, 28, 6, 40, 19, 320)), 0x4ce091a972675f80L);
        assertEquals(DotNETDate.toBinary(getCalendar(2448, 7, 26, 16, 18, 53, 8)), 0x4ab821cae0a54d00L);
        assertEquals(DotNETDate.toBinary(getCalendar(1786, 3, 16, 6, 56, 54, 647)), 0x47d1899081f27870L);
        assertEquals(DotNETDate.toBinary(getCalendar(1776, 0, 7, 2, 37, 47, 768)), 0x47c60516b162ff80L);
        assertEquals(DotNETDate.toBinary(getCalendar(2860, 0, 12, 15, 42, 27, 184)), 0x4c8557181f31c700L);
        assertEquals(DotNETDate.toBinary(getCalendar(2421, 2, 28, 12, 41, 29, 368)), 0x4a99656c97cd0980L);
        assertEquals(DotNETDate.toBinary(getCalendar(1690, 0, 6, 11, 34, 49, 415)), 0x47659a82a98ae570L);
    }

    /**
     * Test the roundtripping of dates, especially pre-Gregorian shift dates.
     * <p>
     * The adoption of the Gregorian calendar causes incompatibilities between
     * .NET and Java for dates before October 15, 1582 (the adoption of the
     * calendar in the Holy Roman Empire.) Since we base our conversion off the
     * Unix epoch, a modern value, we have difficulty representing some of these
     * values.
     * <p>
     * Example: Java lacks dates for October 4, 1582 through October 15, 1582,
     * while .NET does have these values. Further, Java and .NET disagree on
     * leap year handling for years before 1582. Thus, for a particular tick
     * value, we get two different calendar dates for values before October 15,
     * 1582.
     * <p>
     * We therefore do not test these dates, however we ensure that small value
     * tick dates (smaller than 0x6ed6223e4344000, which represents the
     * beginning of the Gregorian calendar) get round-tripped correctly through
     * the serializer.
     */
    public void testRoundTrip() {
        assertEquals(0x4000000000000000L, DotNETDate.toBinary(DotNETDate.fromBinary(0x0)));
        assertEquals(0x4000000000000000L, DotNETDate.toBinary(DotNETDate.fromBinary(0x4000000000000000L)));

        assertEquals(0x46EC3F64975CC000L, DotNETDate.toBinary(DotNETDate.fromBinary(0x6EC3F64975CC000L)));
        assertEquals(0x46EC3F64975CC000L, DotNETDate.toBinary(DotNETDate.fromBinary(0x46EC3F64975CC000L)));

        assertEquals(0x40e413b514302f20L, DotNETDate.toBinary(DotNETDate.fromBinary(0xe413b514302f20L)));
        assertEquals(0x4ceb30a7067ff460L, DotNETDate.toBinary(DotNETDate.fromBinary(0xceb30a7067ff460L)));
        assertEquals(0x4b7b374a7c0b2230L, DotNETDate.toBinary(DotNETDate.fromBinary(0xb7b374a7c0b2230L)));
        assertEquals(0x41d0a3df5eb6b0d0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x1d0a3df5eb6b0d0L)));
        assertEquals(0x45bf0859480eb550L, DotNETDate.toBinary(DotNETDate.fromBinary(0x5bf0859480eb550L)));
        assertEquals(0x4c4f97b6d0b4f800L, DotNETDate.toBinary(DotNETDate.fromBinary(0xc4f97b6d0b4f800L)));
        assertEquals(0x4a9d7479e3d75fe0L, DotNETDate.toBinary(DotNETDate.fromBinary(0xa9d7479e3d75fe0L)));
        assertEquals(0x471493275769fc80L, DotNETDate.toBinary(DotNETDate.fromBinary(0x71493275769fc80L)));
        assertEquals(0x4cb89be833e7e790L, DotNETDate.toBinary(DotNETDate.fromBinary(0xcb89be833e7e790L)));
        assertEquals(0x4b8a986d40cd9570L, DotNETDate.toBinary(DotNETDate.fromBinary(0xb8a986d40cd9570L)));
        assertEquals(0x473e200a95028b90L, DotNETDate.toBinary(DotNETDate.fromBinary(0x73e200a95028b90L)));
        assertEquals(0x4804cc7597b8b290L, DotNETDate.toBinary(DotNETDate.fromBinary(0x804cc7597b8b290L)));
        assertEquals(0x44e1335bba25a880L, DotNETDate.toBinary(DotNETDate.fromBinary(0x4e1335bba25a880L)));
        assertEquals(0x42a84c384e546780L, DotNETDate.toBinary(DotNETDate.fromBinary(0x2a84c384e546780L)));
        assertEquals(0x470a4f1789f32ea0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x70a4f1789f32ea0L)));
        assertEquals(0x437aa191110f5070L, DotNETDate.toBinary(DotNETDate.fromBinary(0x37aa191110f5070L)));
        assertEquals(0x40f73eab8e764f60L, DotNETDate.toBinary(DotNETDate.fromBinary(0xf73eab8e764f60L)));
        assertEquals(0x483dd6277cc99db0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x83dd6277cc99db0L)));
        assertEquals(0x40ac24141e27bf20L, DotNETDate.toBinary(DotNETDate.fromBinary(0xac24141e27bf20L)));
        assertEquals(0x47c041513cfe80d0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x7c041513cfe80d0L)));
        assertEquals(0x426ee9a7f8007da0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x26ee9a7f8007da0L)));
        assertEquals(0x41d6350962704130L, DotNETDate.toBinary(DotNETDate.fromBinary(0x1d6350962704130L)));
        assertEquals(0x49f1ef87bef76fd0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x9f1ef87bef76fd0L)));
        assertEquals(0x4b4bd1e94a4fb7b0L, DotNETDate.toBinary(DotNETDate.fromBinary(0xb4bd1e94a4fb7b0L)));
        assertEquals(0x488c51d91b859e10L, DotNETDate.toBinary(DotNETDate.fromBinary(0x88c51d91b859e10L)));
        assertEquals(0x440a08821fae36e0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x40a08821fae36e0L)));
        assertEquals(0x41c58afdfb5aad70L, DotNETDate.toBinary(DotNETDate.fromBinary(0x1c58afdfb5aad70L)));
        assertEquals(0x4c11d4931e52cbc0L, DotNETDate.toBinary(DotNETDate.fromBinary(0xc11d4931e52cbc0L)));
        assertEquals(0x43636e7ab54c4a00L, DotNETDate.toBinary(DotNETDate.fromBinary(0x3636e7ab54c4a00L)));
        assertEquals(0x4691e4297ee74310L, DotNETDate.toBinary(DotNETDate.fromBinary(0x691e4297ee74310L)));
        assertEquals(0x462289fd0ad851d0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x62289fd0ad851d0L)));
        assertEquals(0x40a5629fdcceca00L, DotNETDate.toBinary(DotNETDate.fromBinary(0xa5629fdcceca00L)));
        assertEquals(0x405b3009c822c330L, DotNETDate.toBinary(DotNETDate.fromBinary(0x5b3009c822c330L)));
        assertEquals(0x42484d0cca9ac4f0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x2484d0cca9ac4f0L)));
        assertEquals(0x42b30457fe054f50L, DotNETDate.toBinary(DotNETDate.fromBinary(0x2b30457fe054f50L)));
        assertEquals(0x40c48d3cc510df20L, DotNETDate.toBinary(DotNETDate.fromBinary(0xc48d3cc510df20L)));
        assertEquals(0x40ffdade22c66700L, DotNETDate.toBinary(DotNETDate.fromBinary(0xffdade22c66700L)));
        assertEquals(0x40bbf4138252e7c0L, DotNETDate.toBinary(DotNETDate.fromBinary(0xbbf4138252e7c0L)));
        assertEquals(0x4a0683955628bf20L, DotNETDate.toBinary(DotNETDate.fromBinary(0xa0683955628bf20L)));
        assertEquals(0x41eac5adef19a1f0L, DotNETDate.toBinary(DotNETDate.fromBinary(0x1eac5adef19a1f0L)));
        assertEquals(0x402e2e46481a6d10L, DotNETDate.toBinary(DotNETDate.fromBinary(0x2e2e46481a6d10L)));
        assertEquals(0x4cd1063f77df46d0L, DotNETDate.toBinary(DotNETDate.fromBinary(0xcd1063f77df46d0L)));
        assertEquals(0x49549ee157cd2960L, DotNETDate.toBinary(DotNETDate.fromBinary(0x9549ee157cd2960L)));
        assertEquals(0x4250082377c9a690L, DotNETDate.toBinary(DotNETDate.fromBinary(0x250082377c9a690L)));
        assertEquals(0x449787ff0da5f110L, DotNETDate.toBinary(DotNETDate.fromBinary(0x49787ff0da5f110L)));
    }

    /*
     * .NET can serialize DateTimes as "local" times, that is the number of
     * ticks are in an (unspecified) local time zone. We do not handle these as
     * the result is undefined.
     */
    public void testThrowsForLocalDateTimes() throws Exception {
        final long[] localDateTimes = new long[] {
            0x8000000000000000L,
            0x80df48e99cb258f1L,
            0x86e3b6dc02fbc000L,
            0x888fa830c0bd3810L
        };

        for (final long localDateTime : localDateTimes) {
            try {
                DotNETDate.fromBinary(localDateTime);
            } catch (final Exception e) {
                continue;
            }

            throw new Exception(
                "Unexpectedly parsed time 0x" + Long.toHexString(localDateTime) + " (represents local DateTime)"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private Calendar getCalendar(
        final int year,
        final int month,
        final int day,
        final int hour,
        final int min,
        final int sec,
        final int millis) {
        final Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.AM_PM, Calendar.AM);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, millis);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        return calendar;
    }
}
