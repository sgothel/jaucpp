/*
 * Author: Sven Gothel <sgothel@jausoft.com>
 * Copyright (c) 2021 Gothel Software e.K.
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
#include <iostream>
#include <cassert>
#include <cinttypes>
#include <cstring>

#define CATCH_CONFIG_MAIN
#include <catch2/catch_amalgamated.hpp>

#include <jau/basic_types.hpp>

static constexpr inline bool VERBOSE = false;

/**
 * Test private impl namespace
 */
namespace test_impl {
    template<class Dummy_type>
    constexpr bool isLittleEndian2_impl(std::enable_if_t<jau::has_endian_little_v<Dummy_type>, bool> = true) noexcept {
        return true;
    }

    template<class Dummy_type>
    constexpr bool isLittleEndian2_impl(std::enable_if_t<!jau::has_endian_little_v<Dummy_type>, bool> = true) noexcept {
        return false;
    }
}

/**
 * Just demonstrating usage of our type-traits
 * in a convenient API manner w/o requiring to add the dummy template type.
 */
constexpr bool isLittleEndian2() noexcept {
    return test_impl::isLittleEndian2_impl<bool>();
}


TEST_CASE( "Endianess Test 00", "[endian]" ) {
    fprintf(stderr, "********************************************************************************\n");
    fprintf(stderr, "is_builtin_bit_cast_available: %d\n", jau::is_builtin_bit_cast_available());
    fprintf(stderr, "endian: %s\n", jau::to_string(jau::endian::native).c_str());
    fprintf(stderr, "********************************************************************************\n");

    const bool cpp_is_little =
        #if BYTE_ORDER == LITTLE_ENDIAN
            true;
        #else
            false;
        #endif
    const bool cpp_is_big =
        #if BYTE_ORDER == BIG_ENDIAN
            true;
        #else
            false;
        #endif
    const bool is_little = jau::endian::little == jau::endian::native;
    const bool is_big = jau::endian::big == jau::endian::native;
    REQUIRE( cpp_is_little == is_little );
    REQUIRE( cpp_is_little == jau::isLittleEndian() );
    REQUIRE( cpp_is_big == is_big );
    REQUIRE( is_little == isLittleEndian2());
}

template<typename Value_type>
static void print(const Value_type a) {
    const uint8_t * pa = reinterpret_cast<const uint8_t *>(&a);
    for(std::size_t i=0; i<sizeof(Value_type); i++) {
        fprintf(stderr, "a[%zu] 0x%X, ", i, pa[i]);
    }
}

template<typename Value_type>
static bool compare(const Value_type a, const Value_type b) {
    const uint8_t * pa = reinterpret_cast<const uint8_t *>(&a);
    const uint8_t * pb = reinterpret_cast<const uint8_t *>(&b);
    bool res = true;
    for(std::size_t i=0; i<sizeof(Value_type) && res; i++) {
        res = pa[i] == pb[i];
        if( !res ) {
            fprintf(stderr, "pa[%zu] 0x%X != pb[%zu] 0x%X\n", i, pa[i], i, pb[i]);
        }
    }
    return res;
}

template<typename Value_type>
static void test_byteorder(const Value_type v_cpu,
                           const Value_type v_le,
                           const Value_type v_be)
{
    if( VERBOSE ) {
        fprintf(stderr, "test_byteorder: sizeof %zu; platform littleEndian %d", sizeof(Value_type), jau::isLittleEndian());
        fprintf(stderr, "\ncpu: %s: ", jau::to_hexstring(v_cpu).c_str()); print(v_cpu);
        fprintf(stderr, "\nle_: %s: ", jau::to_hexstring(v_le).c_str()); print(v_le);
        fprintf(stderr, "\nbe_: %s: ", jau::to_hexstring(v_be).c_str()); print(v_be);
        fprintf(stderr, "\n");
    }
    {
        Value_type r1_le = jau::bswap(v_be);
        REQUIRE( r1_le == v_le );
        Value_type r1_be = jau::bswap(v_le);
        REQUIRE( r1_be == v_be );
    }
    {
        #if BYTE_ORDER == LITTLE_ENDIAN
            REQUIRE( compare(v_le, v_cpu) == true );
            Value_type r1_cpu = jau::bswap(v_be);
            REQUIRE( r1_cpu == v_cpu );
        #else
            REQUIRE( compare(v_be, v_cpu) == true );
            Value_type r1_cpu = jau::bswap(v_le);
            REQUIRE( r1_cpu == v_cpu );
        #endif
    }
    {
        Value_type r1_cpu = jau::le_to_cpu(v_le);
        Value_type r2_cpu = jau::be_to_cpu(v_be);
        REQUIRE( r1_cpu == v_cpu );
        REQUIRE( r2_cpu == v_cpu );
    }
}

static uint16_t compose(const uint8_t n1, const uint8_t n2) {
    uint16_t dest;
    uint8_t * p_dest = reinterpret_cast<uint8_t*>(&dest);
    p_dest[0] = n1;
    p_dest[1] = n2;
    return dest;
}
static uint32_t compose(const uint8_t n1, const uint8_t n2, const uint8_t n3, const uint8_t n4) {
    uint32_t dest;
    uint8_t * p_dest = reinterpret_cast<uint8_t*>(&dest);
    p_dest[0] = n1;
    p_dest[1] = n2;
    p_dest[2] = n3;
    p_dest[3] = n4;
    return dest;
}
static uint64_t compose(const uint8_t n1, const uint8_t n2, const uint8_t n3, const uint8_t n4,
                        const uint8_t n5, const uint8_t n6, const uint8_t n7, const uint8_t n8) {
    uint64_t dest;
    uint8_t * p_dest = reinterpret_cast<uint8_t*>(&dest);
    p_dest[0] = n1;
    p_dest[1] = n2;
    p_dest[2] = n3;
    p_dest[3] = n4;
    p_dest[4] = n5;
    p_dest[5] = n6;
    p_dest[6] = n7;
    p_dest[7] = n8;
    return dest;
}

template<typename Value_type>
static Value_type compose(const uint8_t lowest_value, const bool little_endian) {
    Value_type dest;
    uint8_t * p_dest = reinterpret_cast<uint8_t*>(&dest);
    uint8_t byte_value = lowest_value;
    if( little_endian ) {
        for(size_t i=0; i<sizeof(dest); i++, byte_value++) {
            p_dest[i] = byte_value;
        }
    } else {
        for(ssize_t i=sizeof(dest)-1; i>=0; i--, byte_value++) {
            p_dest[i] = byte_value;
        }
    }
    return dest;
}

TEST_CASE( "Integer Type Byte Order Test 01", "[byteorder][bswap]" ) {
    {
        uint16_t cpu = 0x3210U;
        uint16_t le = compose(0x10, 0x32); // stream: 1032
        uint16_t be = compose(0x32, 0x10); // stream: 3210
        test_byteorder(cpu, le, be);
    }
    {
        uint32_t cpu = 0x76543210U;
        uint32_t le = compose(0x10, 0x32, 0x54, 0x76); // stream: 10325476
        uint32_t be = compose(0x76, 0x54, 0x32, 0x10); // stream: 76543210
        test_byteorder(cpu, le, be);
    }
    {
        uint64_t cpu = 0xfedcba9876543210ULL;
        uint64_t le = compose(0x10, 0x32, 0x54, 0x76, 0x98, 0xba, 0xdc, 0xfe); // stream: 1032547698badcfe
        uint64_t be = compose(0xfe, 0xdc, 0xba, 0x98, 0x76, 0x54, 0x32, 0x10); // stream: fedcba9876543210
        test_byteorder(cpu, le, be);
    }
    {
        jau::uint128_t le = compose<jau::uint128_t>(0x01, true /* little_endian */);
        jau::uint128_t be = compose<jau::uint128_t>(0x01, false /* little_endian */);
        jau::uint128_t cpu = jau::isLittleEndian() ? le : be;
        test_byteorder(cpu, le, be);
    }
    {
        jau::uint192_t le = compose<jau::uint192_t>(0x01, true /* little_endian */);
        jau::uint192_t be = compose<jau::uint192_t>(0x01, false /* little_endian */);
        jau::uint192_t cpu = jau::isLittleEndian() ? le : be;
        test_byteorder(cpu, le, be);
    }
    {
        jau::uint256_t le = compose<jau::uint256_t>(0x01, true /* little_endian */);
        jau::uint256_t be = compose<jau::uint256_t>(0x01, false /* little_endian */);
        jau::uint256_t cpu = jau::isLittleEndian() ? le : be;
        test_byteorder(cpu, le, be);
    }
}

template<typename Value_type>
static void test_value_cpu(const Value_type v1, const Value_type v2, const Value_type v3) {
    uint8_t buffer[3 * sizeof(Value_type)];
    jau::put_value(buffer, sizeof(Value_type)*0, v1);
    jau::put_value(buffer, sizeof(Value_type)*1, v2);
    jau::put_value(buffer, sizeof(Value_type)*2, v3);
    const Value_type r1 = jau::get_value<Value_type>(buffer, sizeof(Value_type)*0);
    const Value_type r2 = jau::get_value<Value_type>(buffer, sizeof(Value_type)*1);
    const Value_type r3 = jau::get_value<Value_type>(buffer, sizeof(Value_type)*2);
    REQUIRE( r1 == v1);
    REQUIRE( r2 == v2);
    REQUIRE( r3 == v3);
}

TEST_CASE( "Integer Get/Put in CPU Byte Order Test 02", "[byteorder][get][put]" ) {
    {
        uint8_t a = 0x01, b = 0x11, c = 0xff;
        test_value_cpu(a, b, c);
    }
    {
        uint16_t a = 0x0123, b = 0x1122, c = 0xffee;
        test_value_cpu(a, b, c);
    }
    {
        uint32_t a = 0x01234567U, b = 0x11223344U, c = 0xffeeddccU;
        test_value_cpu(a, b, c);
    }
    {
        uint64_t a = 0x0123456789abcdefULL, b = 0x1122334455667788ULL, c = 0xffeeddcc99887766ULL;
        test_value_cpu(a, b, c);
    }
    {
        jau::uint128_t a = compose<jau::uint128_t>(0x01, jau::isLittleEndian());
        jau::uint128_t b = compose<jau::uint128_t>(0x20, jau::isLittleEndian());
        jau::uint128_t c = compose<jau::uint128_t>(0x40, jau::isLittleEndian());
        test_value_cpu(a, b, c);
    }
    {
        jau::uint192_t a = compose<jau::uint192_t>(0x01, jau::isLittleEndian());
        jau::uint192_t b = compose<jau::uint192_t>(0x20, jau::isLittleEndian());
        jau::uint192_t c = compose<jau::uint192_t>(0x40, jau::isLittleEndian());
        test_value_cpu(a, b, c);
    }
    {
        jau::uint256_t a = compose<jau::uint256_t>(0x01, jau::isLittleEndian());
        jau::uint256_t b = compose<jau::uint256_t>(0x20, jau::isLittleEndian());
        jau::uint256_t c = compose<jau::uint256_t>(0x40, jau::isLittleEndian());
        test_value_cpu(a, b, c);
    }
}

template<typename Value_type>
static void test_value_littlebig(const Value_type v_cpu, const Value_type v_le, const Value_type v_be) {
    if( VERBOSE ) {
        fprintf(stderr, "test_value_littlebig: sizeof %zu; platform littleEndian %d", sizeof(Value_type), jau::isLittleEndian());
        fprintf(stderr, "\ncpu: %s: ", jau::to_hexstring(v_cpu).c_str()); print(v_cpu);
        fprintf(stderr, "\nle_: %s: ", jau::to_hexstring(v_le).c_str()); print(v_le);
        fprintf(stderr, "\nbe_: %s: ", jau::to_hexstring(v_be).c_str()); print(v_be);
        fprintf(stderr, "\n");
    }
    uint8_t buffer[2 * sizeof(Value_type)];

    jau::put_value(buffer, sizeof(Value_type)*0, v_cpu, true /* little_endian */);
    jau::put_value(buffer, sizeof(Value_type)*1, v_cpu, false /* little_endian */);

    const Value_type rle_raw = jau::get_value<Value_type>(buffer, sizeof(Value_type)*0);
    const Value_type rle_cpu = jau::get_value<Value_type>(buffer, sizeof(Value_type)*0, true /* little_endian */);
    REQUIRE( rle_raw == v_le);
    REQUIRE( rle_cpu == v_cpu);

    const Value_type rbe_raw = jau::get_value<Value_type>(buffer, sizeof(Value_type)*1);
    const Value_type rbe_cpu = jau::get_value<Value_type>(buffer, sizeof(Value_type)*1, false /* little_endian */);
    REQUIRE( rbe_raw == v_be);
    REQUIRE( rbe_cpu == v_cpu);
}

TEST_CASE( "Integer Get/Put in explicit Byte Order Test 03", "[byteorder][get][put]" ) {
    {
        uint16_t cpu = 0x3210U;
        uint16_t le = compose(0x10, 0x32); // stream: 1032
        uint16_t be = compose(0x32, 0x10); // stream: 3210
        test_value_littlebig(cpu, le, be);
    }
    {
        uint32_t cpu = 0x76543210U;
        uint32_t le = compose(0x10, 0x32, 0x54, 0x76); // stream: 10325476
        uint32_t be = compose(0x76, 0x54, 0x32, 0x10); // stream: 76543210
        test_value_littlebig(cpu, le, be);
    }
    {
        uint64_t cpu = 0xfedcba9876543210ULL;
        uint64_t le = compose(0x10, 0x32, 0x54, 0x76, 0x98, 0xba, 0xdc, 0xfe); // stream: 1032547698badcfe
        uint64_t be = compose(0xfe, 0xdc, 0xba, 0x98, 0x76, 0x54, 0x32, 0x10); // stream: fedcba9876543210
        test_value_littlebig(cpu, le, be);
    }
    {
        jau::uint128_t le = compose<jau::uint128_t>(0x01, true /* little_endian */);
        jau::uint128_t be = compose<jau::uint128_t>(0x01, false /* little_endian */);
        jau::uint128_t cpu = jau::isLittleEndian() ? le : be;
        test_value_littlebig(cpu, le, be);
    }
    {
        jau::uint192_t le = compose<jau::uint192_t>(0x01, true /* little_endian */);
        jau::uint192_t be = compose<jau::uint192_t>(0x01, false /* little_endian */);
        jau::uint192_t cpu = jau::isLittleEndian() ? le : be;
        test_value_littlebig(cpu, le, be);
    }
    {
        jau::uint256_t le = compose<jau::uint256_t>(0x01, true /* little_endian */);
        jau::uint256_t be = compose<jau::uint256_t>(0x01, false /* little_endian */);
        jau::uint256_t cpu = jau::isLittleEndian() ? le : be;
        test_value_littlebig(cpu, le, be);
    }
}

