package com.swiftlicious.hellblock.generation;

public enum IslandOptions {

	DEFAULT("default"), CLASSIC("classic"), SCHEMATIC("schematic");
	
	private String name;
	
	IslandOptions(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
}
