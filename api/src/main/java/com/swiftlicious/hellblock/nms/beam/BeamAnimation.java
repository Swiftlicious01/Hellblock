package com.swiftlicious.hellblock.nms.beam;

public interface BeamAnimation {
	
	/**
	 * Executes one frame of the beam animation.
	 */
	void run();

	/**
	 * @return true if the beam animation has finished and should be removed/stopped
	 */
	boolean isFinished();
}