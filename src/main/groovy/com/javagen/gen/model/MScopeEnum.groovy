package com.javagen.gen.model

import static com.javagen.gen.util.GlobalFunctionsUtil.*;

enum MScopeEnum
{
    Private,
    Package,
    Protected,
    Public;
    String toString() { lowerCase(name()) }
}