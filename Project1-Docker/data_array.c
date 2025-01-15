#include "data_array.h"
#include <assert.h>
#include <stdlib.h>
#include <string.h>

/** Data array structure to store array of string values */
struct data_array {
    char **data; // Pointer to existing data array
    size_t size; // Number of elements in the data array
    unsigned int capacity; // Maximum number of elements in the data array
};

/** Initialize a new data array */
data_array_t *data_arr_init() {
    data_array_t *arr = (data_array_t *)malloc(sizeof(data_array_t));
    arr->size = 0;
    arr->capacity = 2;
    arr->data = (char **)malloc(arr->capacity * sizeof(char *));

    return arr;
}

/** Get the number of elements in the data array */
const size_t data_arr_size(data_array_t *arr) {
    assert(arr != NULL);

    return arr->size;
}

/** Get the maximum number of elements in the data array */
const unsigned int data_arr_capacity(data_array_t *arr) {
    assert(arr != NULL);

    return arr->capacity;
}

/** Get the element at the specified index in the data array */
const char *data_arr_get(data_array_t *arr, unsigned int idx) {
    assert(arr != NULL);
    assert(idx < arr->size);

    return arr->data[idx];
}

/** Add a new element to the back of the data array */
void data_arr_add(data_array_t *arr, const char *element) {
    assert(arr != NULL);

    if (arr->size == arr->capacity) {
        arr->capacity *= 2;
        arr->data = (char **)realloc(arr->data, arr->capacity * sizeof(char *));
    }

    char *str = (char *)malloc(strlen(element) + 1);
    strcpy(str, element);
    arr->data[arr->size++] = str;
}

/** Remove an element from the data array */
void data_arr_remove(data_array_t *arr, const char *element) {
    assert(arr != NULL);

    for (int i = 0; i < arr->size; i++) {
        if (strcmp(arr->data[i], element) == 0) {
            free(arr->data[i]);
            for (int j = i; j < arr->size - 1; j++) {
                arr->data[j] = arr->data[j + 1];
            }
            arr->size--;

            break;
        }
    }
}

/** Obliterate the data array, freeing all the memory it occupies */
void data_arr_obliterate(data_array_t *arr) {
    assert(arr != NULL);

    for (int i = 0; i < arr->size; i++) {
        free(arr->data[i]);
    }
    free(arr->data);
    free(arr);
}

/** Compare two data arrays based on size and same elements (in no particular order) */
int data_arr_equals(data_array_t *arr1, data_array_t *arr2) {
    assert(arr1 != NULL);
    assert(arr2 != NULL);

    if (arr1->size != arr2->size) {
        return 0;
    }

    for (int i = 0; i < arr1->size; i++) {
        if (data_arr_contains(arr2, arr1->data[i]) == 0) {
            return 0;
        }
    }

    return 1;
}

/** Check if the data array contains a specific element */
int data_arr_contains(data_array_t *arr, const char *element) {
    assert(arr != NULL);

    for (int i = 0; i < arr->size; i++) {
        if (strcmp(arr->data[i], element) == 0) {
            return 1;
        }
    }

    return 0;
}

/** Copy the data array; the caller is responsible for freeing the memory associated with
 * the copy */
data_array_t *data_arr_copy(data_array_t *arr) {
	assert(arr != NULL);

    data_array_t *copy = data_arr_init();
    for (int i = 0; i < arr->size; i++) {
        data_arr_add(copy, arr->data[i]);
    }

    return copy;
}