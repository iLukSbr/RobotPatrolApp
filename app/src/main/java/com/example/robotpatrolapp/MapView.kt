package com.example.robotpatrolapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import com.example.robotpatrolapp.model.RobotPosition
import kotlin.math.cos
import kotlin.math.sin

class MapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var robotPosition: RobotPosition = RobotPosition(0f, 0f, 0f)
    private val robotPath = Path()
    private var scaleFactor = 1f  // Initial zoom level
    private val zoomOutFactor = 0.9f  // How much to zoom out each time
    //private val robotPathMeasure = PathMeasure(robotPath, false)

    private val robotPaint: Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val pathPaint: Paint = Paint().apply {
        color = Color.RED
        strokeWidth = 30f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        setShadowLayer(10f, 0f, 0f, Color.LTGRAY)
        isAntiAlias = true
    }

    private val arrowPaint = Paint().apply {
        color = Color.GREEN  // Arrow color
        strokeWidth = 8f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Method to set a new starting position
    fun setStartingPosition(x: Float, y: Float, theta : Float) {
        robotPosition.x = x
        robotPosition.y = y
        robotPosition.theta = theta

        robotPath.moveTo(x, y)  // Start path from the initial position
        setLayerType(LAYER_TYPE_SOFTWARE,null)
        invalidate()  // Refresh the canvas
    }

    // Call this method to update robot position
    fun updatePosition(newPosition: RobotPosition) {
        robotPath.lineTo(newPosition.x, newPosition.y)

        robotPosition.x = newPosition.x
        robotPosition.y = newPosition.y
        robotPosition.theta = newPosition.theta

        checkBounds()

        //robotPathMeasure.setPath(robotPath, false)
        //robotPathMeasure.getPosTan(robotPathMeasure.length, (floatArrayOf(robotPosition.x,robotPosition.y)), null)
        invalidate()  // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val canvasX = robotPosition.x //
        val canvasY = robotPosition.y //


        // Apply zoom-out effect
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)

        // Draw the robot as a red circle
        canvas.drawCircle(canvasX, canvasY, 40f, robotPaint)

        // Draw path
        canvas.drawPath(robotPath, pathPaint)

        //canvas.drawLine(canvasX, canvasY, endX, endY, orientationPaint)
        drawArrow(canvas, canvasX, canvasY, robotPosition.theta)
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, angle: Float) {
        val arrowSize = 40f  // Length of the arrow
        val arrowPath = Path()

        // Define the arrow shape (triangle)
        arrowPath.moveTo(0f, -arrowSize)   // Arrow tip
        arrowPath.lineTo(-arrowSize / 2, arrowSize / 2)  // Left wing
        arrowPath.lineTo(arrowSize / 2, arrowSize / 2)   // Right wing
        arrowPath.close()

        // Save canvas state before rotation
        canvas.save()

        // Move canvas to robot's position and rotate
        canvas.translate(x, y)
        canvas.rotate(angle)

        // Draw the rotated arrow
        canvas.drawPath(arrowPath, arrowPaint)

        // Restore canvas state after drawing
        canvas.restore()
    }

    private fun checkBounds() {
        // Check if the robot is near the edge
        if (robotPosition.x < -((width/scaleFactor) - width)/2 || robotPosition.x > ((width/scaleFactor) - width)/2 + width || robotPosition.y < -((height/scaleFactor) - height)/2 || robotPosition.y > ((height/scaleFactor) - height)/2 + height) {
            scaleFactor *= zoomOutFactor  // Reduce scale factor to zoom out
        }
    }

    // Call this to get robot position
    fun getPositionX():  Float{
        return robotPosition.x
    }

    fun getPositionY():  Float{
        return robotPosition.y
    }

    fun getPositionTheta():  Float{
        return robotPosition.theta
    }

    fun resetCanvas() {
        robotPath.reset()  // Clear the drawn path
        robotPosition = RobotPosition(width/2f, height/2f, 0f)
        scaleFactor = 1f
        invalidate()  // Force redraw
    }
}