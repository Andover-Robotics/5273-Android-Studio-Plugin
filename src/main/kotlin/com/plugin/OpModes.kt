package com.plugin

private fun javaOpMode(name: String, opmode: String, group: String, teleop: Boolean, isLinear: Boolean): String {
    val annotations = ArrayList<String>()
    if (opmode.isNotEmpty()) annotations.add("name = \"$opmode\"")
    if (group.isNotEmpty()) annotations.add("group = \"$group\"")
    val type = if (teleop) "TeleOp" else "Autonomous"

    if (isLinear) return """
    import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
    import com.qualcomm.robotcore.eventloop.opmode.$type;

    @$type(${annotations.joinToString(", ")})
    class $name extends LinearOpMode {
        @Override
        public void runOpMode() {
            waitForStart();
            while (opModeIsActive()) {
                // fill in code
            }
        }
    }
    """.trimIndent()

    return """
    import com.qualcomm.robotcore.eventloop.opmode.OpMode;
    import com.qualcomm.robotcore.eventloop.opmode.$type;

    @$type(${annotations.joinToString(", ")})
    class $name extends OpMode {
        @Override
        public void init() {
            // put code here
        }
        
        @Override
        public void loop() {
            // put code here
        }
    }
    """.trimIndent()
}

private fun kotlinOpMode(name: String, opmode: String, group: String, teleop: Boolean, isLinear: Boolean): String {
    val annotations = ArrayList<String>()
    if (opmode.isNotEmpty()) annotations.add("name = \"$opmode\"")
    if (group.isNotEmpty()) annotations.add("group = \"$group\"")
    val type = if (teleop) "TeleOp" else "Autonomous"

    if (isLinear) return """
    import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
    import com.qualcomm.robotcore.eventloop.opmode.$type

    @$type(${annotations.joinToString(", ")})
    class $name: LinearOpMode() {
        override fun runOpMode() {
            waitForStart()
            while (opModeIsActive()) {
                // fill in code
            }
        }
    }
    """.trimIndent()

    return """
    import com.qualcomm.robotcore.eventloop.opmode.OpMode
    import com.qualcomm.robotcore.eventloop.opmode.$type

    @$type(${annotations.joinToString(", ")})
    class $name: OpMode() {
        override fun init() {
            // put code here
        }
        
        override fun loop() {
            // put code here
        }
    }
    """.trimIndent()
}

fun createOpMode(java: Boolean, name: String, opmode: String, group: String, teleop: Boolean, isLinear: Boolean): String {
    if (java) return javaOpMode(name, opmode, group, teleop, isLinear)
    return kotlinOpMode(name, opmode, group, teleop, isLinear)
}