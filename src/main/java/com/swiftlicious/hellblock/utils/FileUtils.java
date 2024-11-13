package com.swiftlicious.hellblock.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for handling file and directory operations.
 */
public class FileUtils {

	private FileUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Creates a file if it does not already exist.
	 *
	 * @param path the path to the file
	 * @return the path to the file
	 * @throws IOException if an I/O error occurs
	 */
	public static Path createFileIfNotExists(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createFile(path);
		}
		return path;
	}

	/**
	 * Creates a directory if it does not already exist.
	 *
	 * @param path the path to the directory
	 * @return the path to the directory
	 * @throws IOException if an I/O error occurs
	 */
	public static Path createDirectoryIfNotExists(Path path) throws IOException {
		if (Files.exists(path) && (Files.isDirectory(path) || Files.isSymbolicLink(path))) {
			return path;
		}

		try {
			Files.createDirectory(path);
		} catch (FileAlreadyExistsException e) {
			// ignore
		}

		return path;
	}

	/**
	 * Creates directories if they do not already exist.
	 *
	 * @param path the path to the directories
	 * @return the path to the directories
	 * @throws IOException if an I/O error occurs
	 */
	public static Path createDirectoriesIfNotExists(Path path) throws IOException {
		if (Files.exists(path) && (Files.isDirectory(path) || Files.isSymbolicLink(path))) {
			return path;
		}

		try {
			Files.createDirectories(path);
		} catch (FileAlreadyExistsException e) {
			// ignore
		}

		return path;
	}

	/**
	 * Deletes a directory and all its contents.
	 *
	 * @param path the path to the directory
	 * @throws IOException if an I/O error occurs
	 */
	public static void deleteDirectory(Path path) throws IOException {
		if (!Files.exists(path) || !Files.isDirectory(path)) {
			return;
		}

		try (DirectoryStream<Path> contents = Files.newDirectoryStream(path)) {
			for (Path file : contents) {
				if (Files.isDirectory(file)) {
					deleteDirectory(file);
				} else {
					Files.delete(file);
				}
			}
		}

		Files.delete(path);
	}
}