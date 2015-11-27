/*
 * Author: Andrei Vasiliu <andrei.vasiliu@intel.com>
 * Copyright (c) 2015 Intel Corporation.
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

#include "tinyb_utils.hpp"

std::vector<unsigned char> tinyb::from_gbytes_to_vector(const GBytes *bytes)
{
    gsize result_size;
    const unsigned char *aux_array = (const unsigned char *)g_bytes_get_data(const_cast<GBytes *>(bytes), &result_size);

    std::vector<unsigned char> result(result_size);
    std::copy(aux_array, aux_array + result_size, result.begin());

    return result;
}

/* it allocates memory - the result that is being returned is from heap */
GBytes *tinyb::from_vector_to_gbytes(const std::vector<unsigned char>& vector)
{
    unsigned int vector_size = vector.size();
    const unsigned char *vector_content = vector.data();

    GBytes *result = g_bytes_new(vector_content, vector_size);
    if (!result)
    {
        g_printerr("Error: cannot allocate\n");
        throw std::exception();
    }

    return result;
}
