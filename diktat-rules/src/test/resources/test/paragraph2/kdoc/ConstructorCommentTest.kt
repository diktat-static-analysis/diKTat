package test.paragraph2.kdoc

/**
 * Simple class
 */
class A constructor(
        // should replace
        name: String
)

/**
 * @property age age
 */
class A(
        val age: Int,
        //Ivanov
        val lastName: String
) {

}

class Out{
    /**
     * Inner class
     */
    class In(
            //Jack
            name: String
    ){}
}
