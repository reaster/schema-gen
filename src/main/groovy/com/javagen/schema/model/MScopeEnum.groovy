package com.javagen.schema.model

import static com.javagen.schema.common.GlobalFunctionsUtil.*

enum MScopeEnum
{
    Private,
    Package,
    Protected,
    Public
    String toString() { lowerCase(name()) }
}