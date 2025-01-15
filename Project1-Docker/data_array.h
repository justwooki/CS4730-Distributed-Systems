#ifndef DATA_ARRAY_H
#define DATA_ARRAY_H

#include <stdlib.h> // needed for size_t

/** Data array structure */
typedef struct data_array data_array_t;

/** Initialize a new data array */
data_array_t *data_arr_init();

/** Get the number of elements in the data array */
const size_t data_arr_size(data_array_t *arr);

/** Get the maximum number of elements in the data array */
const unsigned int data_arr_capacity(data_array_t *arr);

/** Get the element at the specified index in the data array */
const char *data_arr_get(data_array_t *arr, unsigned int idx);

/** Add a new element to the back of the data array */
void data_arr_add(data_array_t *arr, const char *element);

/** Remove an element from the data array */
void data_arr_remove(data_array_t *arr, const char *element);

/** Obliterate the data array, freeing all the memory it occupies */
void data_arr_obliterate(data_array_t *arr);

/** Compare two data arrays based on size and same elements (in no particular order) */
int data_arr_equals(data_array_t *arr1, data_array_t *arr2);

/** Check if the data array contains a specific element */
int data_arr_contains(data_array_t *arr, const char *element);

/** Copy the data array; the caller is responsible for freeing the memory associated with
 * the copy */
data_array_t *data_arr_copy(data_array_t *arr);

#endif // DATA_ARRAY_H