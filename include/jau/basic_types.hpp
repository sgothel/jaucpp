/*
 * Author: Sven Gothel <sgothel@jausoft.com>
 * Copyright (c) 2020 Gothel Software e.K.
 * Copyright (c) 2020 ZAFENA AB
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

#ifndef JAU_BASIC_TYPES_HPP_
#define JAU_BASIC_TYPES_HPP_

#include <cstring>
#include <string>
#include <memory>
#include <cstdint>
#include <vector>
#include <type_traits>

#include <jau/cpp_lang_util.hpp>
#include <jau/packed_attribute.hpp>
#include <jau/type_traits_queries.hpp>

#include <jau/int_types.hpp>
#include <jau/int_math.hpp>
#include <jau/byte_util.hpp>
#include <jau/string_util.hpp>

namespace jau {

    /**
     * Returns current monotonic time in milliseconds.
     */
    uint64_t getCurrentMilliseconds() noexcept;

    /**
     * Returns current wall-clock system `time of day` in seconds since Unix Epoch
     * `00:00:00 UTC on 1 January 1970`.
     */
    uint64_t getWallClockSeconds() noexcept;

    /**
    // *************************************************
    // *************************************************
    // *************************************************
     */

    #define E_FILE_LINE __FILE__,__LINE__

    class RuntimeException : public std::exception {
      private:
        std::string msg;
        std::string backtrace;
        std::string what_;

      protected:
        RuntimeException(std::string const type, std::string const m, const char* file, int line) noexcept;

      public:
        RuntimeException(std::string const m, const char* file, int line) noexcept
        : RuntimeException("RuntimeException", m, file, line) {}

        virtual ~RuntimeException() noexcept { }

        RuntimeException(const RuntimeException &o) = default;
        RuntimeException(RuntimeException &&o) = default;
        RuntimeException& operator=(const RuntimeException &o) = default;
        RuntimeException& operator=(RuntimeException &&o) = default;

        std::string get_backtrace() const noexcept { return backtrace; }

        virtual const char* what() const noexcept override {
            return what_.c_str(); // return std::runtime_error::what();
        }
    };

    class InternalError : public RuntimeException {
      public:
        InternalError(std::string const m, const char* file, int line) noexcept
        : RuntimeException("InternalError", m, file, line) {}
    };

    class OutOfMemoryError : public RuntimeException {
      public:
        OutOfMemoryError(std::string const m, const char* file, int line) noexcept
        : RuntimeException("OutOfMemoryError", m, file, line) {}
    };

    class NullPointerException : public RuntimeException {
      public:
        NullPointerException(std::string const m, const char* file, int line) noexcept
        : RuntimeException("NullPointerException", m, file, line) {}
    };

    class IllegalArgumentException : public RuntimeException {
      public:
        IllegalArgumentException(std::string const m, const char* file, int line) noexcept
        : RuntimeException("IllegalArgumentException", m, file, line) {}
    };

    class IllegalStateException : public RuntimeException {
      public:
        IllegalStateException(std::string const m, const char* file, int line) noexcept
        : RuntimeException("IllegalStateException", m, file, line) {}
    };

    class UnsupportedOperationException : public RuntimeException {
      public:
        UnsupportedOperationException(std::string const m, const char* file, int line) noexcept
        : RuntimeException("UnsupportedOperationException", m, file, line) {}
    };

    class IndexOutOfBoundsException : public RuntimeException {
      public:
        IndexOutOfBoundsException(const std::size_t index, const std::size_t length, const char* file, int line) noexcept
        : RuntimeException("IndexOutOfBoundsException", "Index "+std::to_string(index)+", data length "+std::to_string(length), file, line) {}

        IndexOutOfBoundsException(const std::string index_s, const std::string length_s, const char* file, int line) noexcept
        : RuntimeException("IndexOutOfBoundsException", "Index "+index_s+", data length "+length_s, file, line) {}

        IndexOutOfBoundsException(const std::size_t index, const std::size_t count, const std::size_t length, const char* file, int line) noexcept
        : RuntimeException("IndexOutOfBoundsException", "Index "+std::to_string(index)+", count "+std::to_string(count)+", data length "+std::to_string(length), file, line) {}
    };

    /**
    // *************************************************
    // *************************************************
    // *************************************************
     */

    inline void set_bit_uint32(const uint8_t nr, uint32_t &mask)
    {
        if( nr > 31 ) { throw IndexOutOfBoundsException(nr, 32, E_FILE_LINE); }
        mask |= 1 << (nr & 31);
    }

    inline void clear_bit_uint32(const uint8_t nr, uint32_t &mask)
    {
        if( nr > 31 ) { throw IndexOutOfBoundsException(nr, 32, E_FILE_LINE); }
        mask |= ~(1 << (nr & 31));
    }

    inline uint32_t test_bit_uint32(const uint8_t nr, const uint32_t mask)
    {
        if( nr > 31 ) { throw IndexOutOfBoundsException(nr, 32, E_FILE_LINE); }
        return mask & (1 << (nr & 31));
    }

    inline void set_bit_uint64(const uint8_t nr, uint64_t &mask)
    {
        if( nr > 63 ) { throw IndexOutOfBoundsException(nr, 64, E_FILE_LINE); }
        mask |= 1 << (nr & 63);
    }

    inline void clear_bit_uint64(const uint8_t nr, uint64_t &mask)
    {
        if( nr > 63 ) { throw IndexOutOfBoundsException(nr, 64, E_FILE_LINE); }
        mask |= ~(1 << (nr & 63));
    }

    inline uint64_t test_bit_uint64(const uint8_t nr, const uint64_t mask)
    {
        if( nr > 63 ) { throw IndexOutOfBoundsException(nr, 64, E_FILE_LINE); }
        return mask & (1 << (nr & 63));
    }

    /**
    // *************************************************
    // *************************************************
    // *************************************************
     */

    /**
     * Merge the given 'uuid16' into a 'base_uuid' copy at the given little endian 'uuid16_le_octet_index' position.
     * <p>
     * The given 'uuid16' value will be added with the 'base_uuid' copy at the given position.
     * </p>
     * <pre>
     * base_uuid: 00000000-0000-1000-8000-00805F9B34FB
     *    uuid16: DCBA
     * uuid16_le_octet_index: 12
     *    result: 0000DCBA-0000-1000-8000-00805F9B34FB
     *
     * LE: low-mem - FB349B5F8000-0080-0010-0000-ABCD0000 - high-mem
     *                                           ^ index 12
     * LE: uuid16 -> value.data[12+13]
     *
     * BE: low-mem - 0000DCBA-0000-1000-8000-00805F9B34FB - high-mem
     *                   ^ index 2
     * BE: uuid16 -> value.data[2+3]
     * </pre>
     */
    uint128_t merge_uint128(uint16_t const uuid16, uint128_t const & base_uuid, nsize_t const uuid16_le_octet_index);

    /**
     * Merge the given 'uuid32' into a 'base_uuid' copy at the given little endian 'uuid32_le_octet_index' position.
     * <p>
     * The given 'uuid32' value will be added with the 'base_uuid' copy at the given position.
     * </p>
     * <pre>
     * base_uuid: 00000000-0000-1000-8000-00805F9B34FB
     *    uuid32: 87654321
     * uuid32_le_octet_index: 12
     *    result: 87654321-0000-1000-8000-00805F9B34FB
     *
     * LE: low-mem - FB349B5F8000-0080-0010-0000-12345678 - high-mem
     *                                           ^ index 12
     * LE: uuid32 -> value.data[12..15]
     *
     * BE: low-mem - 87654321-0000-1000-8000-00805F9B34FB - high-mem
     *               ^ index 0
     * BE: uuid32 -> value.data[0..3]
     * </pre>
     */
    uint128_t merge_uint128(uint32_t const uuid32, uint128_t const & base_uuid, nsize_t const uuid32_le_octet_index);

} // namespace jau

/** \example test_intdecstring01.cpp
 * This C++ unit test validates the jau::to_decstring implementation
 */

#endif /* JAU_BASIC_TYPES_HPP_ */
