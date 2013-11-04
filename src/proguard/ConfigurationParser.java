/* $Id: ConfigurationParser.java,v 1.5 2003/12/06 22:12:42 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard;

import proguard.classfile.*;
import proguard.classfile.util.*;
import proguard.util.*;

import java.io.*;
import java.net.URL;
import java.util.*;


/**
 * This class parses ProGuard configurations. Configurations can be read from an
 * array of arguments or from a configuration file or URL.
 *
 * @author Eric Lafortune
 */
public class ConfigurationParser
{
    private WordReader reader;
    private String     nextWord;
    private String     lastComments;


    /**
     * Creates a new ConfigurationParser for the given String arguments.
     */
    public ConfigurationParser(String[] args) throws IOException
    {
        reader = new ArgumentWordReader(args);
        readNextWord();
    }


    /**
     * Creates a new ConfigurationParser for the given file name.
     */
    public ConfigurationParser(String fileName) throws IOException
    {
        reader = new FileWordReader(fileName);
        readNextWord();
    }


    /**
     * Creates a new ConfigurationParser for the given URL.
     */
    public ConfigurationParser(URL url) throws IOException
    {
        reader = new FileWordReader(url);
        readNextWord();
    }


    /**
     * Parses and returns the configuration.
     * @param configuration the configuration that is updated as a side-effect.
     * @throws ParseException if the any of the configuration settings contains
     *                        a syntax error.
     * @throws IOException if an IO error occurs while reading a configuration.
     */
    public void parse(Configuration configuration) throws ParseException, IOException
    {
        while (nextWord != null)
        {
            lastComments = reader.lastComments();

            // First include directives.
            if      (ConfigurationConstants.AT_DIRECTIVE                               .startsWith(nextWord) ||
                     ConfigurationConstants.INCLUDE_DIRECTIVE                          .startsWith(nextWord)) parseIncludeArgument();

            // Then configuration options with or without arguments.
            else if (ConfigurationConstants.LIBRARYJARS_OPTION                         .startsWith(nextWord)) configuration.libraryJars                 = parseClassPathArgument(configuration.libraryJars);
            else if (ConfigurationConstants.INJARS_OPTION                              .startsWith(nextWord)) configuration.inJars                      = parseClassPathArgument(configuration.inJars);
            else if (ConfigurationConstants.RESOURCEJARS_OPTION                        .startsWith(nextWord)) configuration.resourceJars                = parseClassPathArgument(configuration.resourceJars);
            else if (ConfigurationConstants.OUTJARS_OPTION                             .startsWith(nextWord)) configuration.outJars                     = parseClassPathArgument(configuration.outJars);
            else if (ConfigurationConstants.KEEP_OPTION                                .startsWith(nextWord)) configuration.keepClassFileOptions        = parseKeepArguments(configuration.keepClassFileOptions, true,  false, false);
            else if (ConfigurationConstants.KEEP_CLASS_MEMBERS_OPTION                  .startsWith(nextWord)) configuration.keepClassFileOptions        = parseKeepArguments(configuration.keepClassFileOptions, false, false, false);
            else if (ConfigurationConstants.KEEP_CLASSES_WITH_MEMBERS_OPTION           .startsWith(nextWord)) configuration.keepClassFileOptions        = parseKeepArguments(configuration.keepClassFileOptions, false, true,  false);
            else if (ConfigurationConstants.KEEP_NAMES_OPTION                          .startsWith(nextWord)) configuration.keepClassFileOptions        = parseKeepArguments(configuration.keepClassFileOptions, true,  false, true);
            else if (ConfigurationConstants.KEEP_CLASS_MEMBER_NAMES_OPTION             .startsWith(nextWord)) configuration.keepClassFileOptions        = parseKeepArguments(configuration.keepClassFileOptions, false, false, true);
            else if (ConfigurationConstants.KEEP_CLASSES_WITH_MEMBER_NAMES_OPTION      .startsWith(nextWord)) configuration.keepClassFileOptions        = parseKeepArguments(configuration.keepClassFileOptions, false, true,  true);
            else if (ConfigurationConstants.KEEP_ATTRIBUTES_OPTION                     .startsWith(nextWord)) configuration.keepAttributes              = parseKeepAttributesArguments(configuration.keepAttributes);
            else if (ConfigurationConstants.RENAME_SOURCE_FILE_ATTRIBUTE_OPTION        .startsWith(nextWord)) configuration.newSourceFileAttribute      = parseOptionalArgument();
            else if (ConfigurationConstants.PRINT_SEEDS_OPTION                         .startsWith(nextWord)) configuration.printSeeds                  = parseOptionalArgument();
            else if (ConfigurationConstants.PRINT_USAGE_OPTION                         .startsWith(nextWord)) configuration.printUsage                  = parseOptionalArgument();
            else if (ConfigurationConstants.PRINT_MAPPING_OPTION                       .startsWith(nextWord)) configuration.printMapping                = parseOptionalArgument();
            else if (ConfigurationConstants.APPLY_MAPPING_OPTION                       .startsWith(nextWord)) configuration.applyMapping                = parseOptionalArgument();
            else if (ConfigurationConstants.VERBOSE_OPTION                             .startsWith(nextWord)) configuration.verbose                     = parseNoArgument(true);
            else if (ConfigurationConstants.DUMP_OPTION                                .startsWith(nextWord)) configuration.dump                        = parseOptionalArgument();
            else if (ConfigurationConstants.IGNORE_WARNINGS_OPTION                     .startsWith(nextWord)) configuration.ignoreWarnings              = parseNoArgument(true);
            else if (ConfigurationConstants.DONT_WARN_OPTION                           .startsWith(nextWord)) configuration.warn                        = parseNoArgument(false);
            else if (ConfigurationConstants.DONT_NOTE_OPTION                           .startsWith(nextWord)) configuration.note                        = parseNoArgument(false);
            else if (ConfigurationConstants.DONT_SHRINK_OPTION                         .startsWith(nextWord)) configuration.shrink                      = parseNoArgument(false);
            else if (ConfigurationConstants.DONT_OBFUSCATE_OPTION                      .startsWith(nextWord)) configuration.obfuscate                   = parseNoArgument(false);
            else if (ConfigurationConstants.DONT_USE_MIXED_CASE_CLASS_NAMES_OPTION     .startsWith(nextWord)) configuration.useMixedCaseClassNames      = parseNoArgument(false);
            else if (ConfigurationConstants.OVERLOAD_AGGRESSIVELY_OPTION               .startsWith(nextWord)) configuration.overloadAggressively        = parseNoArgument(true);
            else if (ConfigurationConstants.DEFAULT_PACKAGE_OPTION                     .startsWith(nextWord)) configuration.defaultPackage              = ClassUtil.internalClassName(parseOptionalArgument());
            else if (ConfigurationConstants.DONT_SKIP_NON_PUBLIC_LIBRARY_CLASSES_OPTION.startsWith(nextWord)) configuration.skipNonPublicLibraryClasses = parseNoArgument(false);
            else
            {
                throw new ParseException("Unknown configuration " + reader.locationDescription());
            }
        }
    }


    private void parseIncludeArgument()
    throws ParseException, IOException
    {
        // Read the configuation file name.
        readNextWord("configuration file name");

        reader.includeWordReader(new FileWordReader(replaceSystemProperties(nextWord)));

        readNextWord();
    }


    private ClassPath parseClassPathArgument(ClassPath classPath)
    throws ParseException, IOException
    {
        // Create a new List if necessary.
        if (classPath == null)
        {
            classPath = new ClassPath();
        }

        while (true)
        {
            // Read the next jar name.
            readNextWord("jar or directory name");

            // Create a new class path entry.
            ClassPathEntry entry = new ClassPathEntry(replaceSystemProperties(nextWord));

            // Read the opening parenthesis or the separator, if any.
            readNextWord();

            // Read the optional filters.
            if (!configurationEnd() &&
                nextWord.equals(ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD))
            {
                // Apply the filter to the class path entry.
                entry.setFilter(ListUtil.commaSeparatedString(parseCommaSeparatedList(false, true, false)));

                // Read the separator, if any.
                readNextWord();
            }

            // Add the entry to the list.
            classPath.add(entry);

            if (configurationEnd())
            {
                return classPath;
            }

            if (!nextWord.equals(ConfigurationConstants.JAR_SEPARATOR_KEYWORD))
            {
                throw new ParseException("Expecting class path separator '" + ConfigurationConstants.JAR_SEPARATOR_KEYWORD +
                                         "' before " + reader.locationDescription());
            }
        }
    }


    private List parseKeepAttributesArguments(List keepAttributes)
    throws ParseException, IOException
    {
        // Create a new List if necessary.
        if (keepAttributes == null)
        {
            keepAttributes = new ArrayList();
        }

        // Read the first attribute name.
        readNextWord();

        // Should we keep all attributes?
        if (configurationEnd())
        {
            keepAttributes.clear();
            return keepAttributes;
        }

        if (nextWord.equals(ConfigurationConstants.ANY_ATTRIBUTE_KEYWORD))
        {
            keepAttributes.clear();
            readNextWord();
            return keepAttributes;
        }

        while (true)
        {
            // Add the attribute name to the list.
            keepAttributes.add(nextWord);

            // Read the separator, if any.
            readNextWord();
            if (configurationEnd())
            {
                break;
            }

            if (!nextWord.equals(ConfigurationConstants.ATTRIBUTE_SEPARATOR_KEYWORD))
            {
                throw new ParseException("Expecting attribute name separator '" + ConfigurationConstants.ATTRIBUTE_SEPARATOR_KEYWORD +
                                         "' before " + reader.locationDescription());
            }

            // Read the next attribute name.
            readNextWord("attribute name");
        }

        return keepAttributes;
    }


    private String parseOptionalArgument()
    throws ParseException, IOException
    {
        readNextWord();

        // Didn't the user specify a file name?
        if (configurationEnd())
        {
            return "";
        }

        String fileName = nextWord;

        readNextWord();

        return fileName;
    }


    private boolean parseNoArgument(boolean value)
    throws ParseException, IOException
    {
        readNextWord();

        return value;
    }


    private List parseKeepArguments(List    keepClassFileOptions,
                                    boolean markClassFiles,
                                    boolean markClassFilesConditionally,
                                    boolean onlyKeepNames)
    throws ParseException, IOException
    {
        // Create a new List if necessary.
        if (keepClassFileOptions == null)
        {
            keepClassFileOptions = new ArrayList();
        }

        // Read and add the keep configuration.
        keepClassFileOptions.add(parseKeepArguments(markClassFiles,
                                                    markClassFilesConditionally,
                                                    onlyKeepNames));

        return keepClassFileOptions;
    }


    private KeepClassFileOption parseKeepArguments(boolean markClassFiles,
                                                   boolean markClassFilesConditionally,
                                                   boolean onlyKeepNames)
    throws ParseException, IOException
    {
        // Remember the comments preceeding this keep option.
        String comments = lastComments;

        // Parse the class flag specification part, if any.
        int requiredSetClassAccessFlags   = 0;
        int requiredUnsetClassAccessFlags = 0;

        while (true)
        {
            readNextWord("keyword '" + ConfigurationConstants.CLASS_KEYWORD + "'" +
                         " or '" + ClassConstants.EXTERNAL_ACC_INTERFACE + "'");

            if (ConfigurationConstants.CLASS_KEYWORD.equals(nextWord))
            {
                // The class keyword. Stop parsing the class flags.
                break;
            }

            // Strip the negating sign, if any.
            String strippedWord = nextWord.startsWith(ConfigurationConstants.NEGATOR_KEYWORD) ?
                nextWord.substring(1) :
                nextWord;

            int accessFlag =
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_PUBLIC)    ? ClassConstants.INTERNAL_ACC_PUBLIC    :
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_FINAL)     ? ClassConstants.INTERNAL_ACC_FINAL     :
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_INTERFACE) ? ClassConstants.INTERNAL_ACC_INTERFACE :
                strippedWord.equals(ClassConstants.EXTERNAL_ACC_ABSTRACT)  ? ClassConstants.INTERNAL_ACC_ABSTRACT  :
                                                                             unknownAccessFlag();
            if (strippedWord == nextWord)
            {
                requiredSetClassAccessFlags   |= accessFlag;
            }
            else
            {
                requiredUnsetClassAccessFlags |= accessFlag;
            }


            if ((requiredSetClassAccessFlags &
                 requiredUnsetClassAccessFlags) != 0)
            {
                throw new ParseException("Conflicting class access modifiers for '" + strippedWord +
                                         "' before " + reader.locationDescription());
            }

            if (ClassConstants.EXTERNAL_ACC_INTERFACE.equals(strippedWord))
            {
                accessFlag = ClassConstants.INTERNAL_ACC_INTERFACE;

                // The interface keyword. Stop parsing the class flags.
                break;
            }
        }

        readNextWord("class name or interface name");
        checkJavaIdentifier("class name or interface name");

        // Parse the class name part.
        String externalClassName = nextWord;
        String className = ConfigurationConstants.ANY_CLASS_KEYWORD.equals(externalClassName) ?
            null :
            ClassUtil.internalClassName(externalClassName);

        readNextWord();

        String extendsClassName = null;
        String asClassName      = null;

        if (!configurationEnd())
        {
            // Parse 'implements ...' or 'extends ...' part, if any.
            if (ConfigurationConstants.IMPLEMENTS_KEYWORD.equals(nextWord) ||
                ConfigurationConstants.EXTENDS_KEYWORD.equals(nextWord))
            {
                readNextWord("class name or interface name");
                checkJavaIdentifier("class name or interface name");
                extendsClassName = ClassUtil.internalClassName(nextWord);

                readNextWord();
            }
        }

        if (!configurationEnd())
        {
            // Parse the 'as ...' part, if any.
            if (ConfigurationConstants.AS_KEYWORD.equals(nextWord))
            {
                readNextWord("new class name");
                checkJavaIdentifier("new class name");
                asClassName = ClassUtil.internalClassName(nextWord);

                readNextWord();
            }
        }

        // Create the essential specification for keeping the class or classes.
        KeepClassFileOption keepClassFileOption =
            new KeepClassFileOption(requiredSetClassAccessFlags,
                                    requiredUnsetClassAccessFlags,
                                    className,
                                    extendsClassName,
                                    asClassName,
                                    markClassFiles,
                                    markClassFilesConditionally,
                                    onlyKeepNames,
                                    comments);


        // Now modify this KeepClassFileOption, for keeping any class members.
        // Parse the class member opening part, if any.
        if (!configurationEnd())
        {
            if (!ConfigurationConstants.OPEN_KEYWORD.equals(nextWord))
            {
                throw new ParseException("Expecting opening '" + ConfigurationConstants.OPEN_KEYWORD +
                                         "' at " + reader.locationDescription());
            }

            while (true)
            {
                // Parse the class member flag specification part, if any.
                int requiredSetMemberAccessFlags   = 0;
                int requiredUnsetMemberAccessFlags = 0;

                while (true)
                {
                    readNextWord("class member description" +
                                 " or closing '" + ConfigurationConstants.CLOSE_KEYWORD + "'");

                    if (requiredSetMemberAccessFlags   == 0 &&
                        requiredUnsetMemberAccessFlags == 0 &&
                        ConfigurationConstants.CLOSE_KEYWORD.equals(nextWord))
                    {
                        // The closing brace. Stop parsing the class members.
                        readNextWord();
                        return keepClassFileOption;
                    }

                    String strippedWord = nextWord.startsWith("!") ?
                        nextWord.substring(1) :
                        nextWord;

                    int accessFlag =
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_PUBLIC)       ? ClassConstants.INTERNAL_ACC_PUBLIC       :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_PRIVATE)      ? ClassConstants.INTERNAL_ACC_PRIVATE      :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_PROTECTED)    ? ClassConstants.INTERNAL_ACC_PROTECTED    :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_STATIC)       ? ClassConstants.INTERNAL_ACC_STATIC       :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_FINAL)        ? ClassConstants.INTERNAL_ACC_FINAL        :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_SYNCHRONIZED) ? ClassConstants.INTERNAL_ACC_SYNCHRONIZED :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_VOLATILE)     ? ClassConstants.INTERNAL_ACC_VOLATILE     :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_TRANSIENT)    ? ClassConstants.INTERNAL_ACC_TRANSIENT    :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_NATIVE)       ? ClassConstants.INTERNAL_ACC_NATIVE       :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_ABSTRACT)     ? ClassConstants.INTERNAL_ACC_ABSTRACT     :
                        strippedWord.equals(ClassConstants.EXTERNAL_ACC_STRICT)       ? ClassConstants.INTERNAL_ACC_STRICT       :
                                                                                        0;
                    if (accessFlag == 0)
                    {
                        // Not a class member flag. Stop parsing them.
                        break;
                    }

                    if (strippedWord == nextWord)
                    {
                        requiredSetMemberAccessFlags   |= accessFlag;
                    }
                    else
                    {
                        requiredUnsetMemberAccessFlags |= accessFlag;
                    }

                    // Make sure the user doesn't try to set and unset the same
                    // access flags simultaneously.
                    if ((requiredSetMemberAccessFlags &
                         requiredUnsetMemberAccessFlags) != 0)
                    {
                        throw new ParseException("Conflicting class member access modifiers for " +
                                                 reader.locationDescription());
                    }
                }

                // Parse the class member type and name part.

                // Did we get a special wildcard?
                if (ConfigurationConstants.ANY_CLASS_MEMBER_KEYWORD.equals(nextWord) ||
                    ConfigurationConstants.ANY_FIELD_KEYWORD       .equals(nextWord) ||
                    ConfigurationConstants.ANY_METHOD_KEYWORD      .equals(nextWord))
                {
                    // Act acording to the type of wildcard..
                    if (ConfigurationConstants.ANY_CLASS_MEMBER_KEYWORD.equals(nextWord))
                    {
                        checkFieldAccessFlags(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags);
                        checkMethodAccessFlags(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags);

                        keepClassFileOption.addField(
                            new KeepClassMemberOption(requiredSetMemberAccessFlags,
                                                      requiredUnsetMemberAccessFlags,
                                                      null,
                                                      null,
                                                      null));
                        keepClassFileOption.addMethod(
                            new KeepClassMemberOption(requiredSetMemberAccessFlags,
                                                      requiredUnsetMemberAccessFlags,
                                                      null,
                                                      null,
                                                      null));
                    }
                    else if (ConfigurationConstants.ANY_FIELD_KEYWORD.equals(nextWord))
                    {
                        checkFieldAccessFlags(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags);

                        keepClassFileOption.addField(
                            new KeepClassMemberOption(requiredSetMemberAccessFlags,
                                                      requiredUnsetMemberAccessFlags,
                                                      null,
                                                      null,
                                                      null));
                    }
                    else if (ConfigurationConstants.ANY_METHOD_KEYWORD.equals(nextWord))
                    {
                        checkMethodAccessFlags(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags);

                        keepClassFileOption.addMethod(
                            new KeepClassMemberOption(requiredSetMemberAccessFlags,
                                                      requiredUnsetMemberAccessFlags,
                                                      null,
                                                      null,
                                                      null));
                    }

                    // We still have to read the closing separator.
                    readNextWord("separator '" + ConfigurationConstants.SEPARATOR_KEYWORD + "'");

                    if (!ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord))
                    {
                        throw new ParseException("Expecting separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                                                 "' before " + reader.locationDescription());
                    }
                }
                else
                {
                    // Make sure we have a proper type.
                    checkJavaIdentifier("class member type");
                    String type = nextWord;

                    readNextWord("class member name");
                    String name = nextWord;

                    // Did we get just one word before the opening parenthesis?
                    if (ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD.equals(name))
                    {
                        // This must be a constructor then.
                        // Make sure the type is a proper constructor name.
                        if (!(type.equals(ClassConstants.INTERNAL_METHOD_NAME_INIT) ||
                              type.equals(externalClassName) ||
                              type.equals(ClassUtil.externalShortClassName(externalClassName))))
                        {
                            throw new ParseException("Expecting type and name " +
                                                     "instead of just '" + type +
                                                     "' before " + reader.locationDescription());
                        }

                        // Assign the fixed constructor type and name.
                        type = ClassConstants.EXTERNAL_TYPE_VOID;
                        name = ClassConstants.INTERNAL_METHOD_NAME_INIT;
                    }
                    else
                    {
                        // It's not a constructor.
                        // Make sure we have a proper name.
                        checkJavaIdentifier("class member name");

                        // Read the opening parenthesis or the separating
                        // semi-colon.
                        readNextWord("opening '" + ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD +
                                     "' or separator '" + ConfigurationConstants.SEPARATOR_KEYWORD + "'");
                    }

                    // Are we looking at a field, a method, or something else?
                    if (ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord))
                    {
                        // It's a field.
                        checkFieldAccessFlags(requiredSetMemberAccessFlags,
                                              requiredUnsetMemberAccessFlags);

                        // We already have a field descriptor.
                        String descriptor = ClassUtil.internalType(type);

                        // Keep the field.
                        keepClassFileOption.addField(
                            new KeepClassMemberOption(requiredSetMemberAccessFlags,
                                                      requiredUnsetMemberAccessFlags,
                                                      name,
                                                      descriptor,
                                                      null));
                    }
                    else if (ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD.equals(nextWord))
                    {
                        // It's a method.
                        checkMethodAccessFlags(requiredSetMemberAccessFlags,
                                               requiredUnsetMemberAccessFlags);

                        // Parse the method arguments.
                        String descriptor =
                            ClassUtil.internalMethodDescriptor(type,
                                                               parseCommaSeparatedList(true, false, true));

                        // Read the separator after the closing parenthesis.
                        readNextWord("separator '" + ConfigurationConstants.SEPARATOR_KEYWORD + "'");

                        if (!ConfigurationConstants.SEPARATOR_KEYWORD.equals(nextWord))
                        {
                            throw new ParseException("Expecting separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                                                     "' before " + reader.locationDescription());
                        }

                        // Keep the method.
                        keepClassFileOption.addMethod(
                            new KeepClassMemberOption(requiredSetMemberAccessFlags,
                                                      requiredUnsetMemberAccessFlags,
                                                      name,
                                                      descriptor,
                                                      null));
                    }
                    else
                    {
                        // It doesn't look like a field or a method.
                        throw new ParseException("Expecting opening '" + ConfigurationConstants.OPEN_ARGUMENTS_KEYWORD +
                                                 "' or separator '" + ConfigurationConstants.SEPARATOR_KEYWORD +
                                                 "' before " + reader.locationDescription());
                    }
                }
            }
        }

        return keepClassFileOption;
    }


    /**
     * Reads a comma-separated list of java identifiers or of file names,
     * optionally ending after the closing parenthesis.
     */
    private List parseCommaSeparatedList(boolean checkJavaIdentifiers,
                                         boolean replaceSystemProperties,
                                         boolean endWithClosingParenthesis)
    throws ParseException, IOException
    {
        List arguments = new ArrayList();

        while (true)
        {
            // Read an argument.
            readNextWord("argument");

            if (endWithClosingParenthesis &&
                arguments.size() == 0     &&
                ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD.equals(nextWord))
            {
                break;
            }

            if (checkJavaIdentifiers)
            {
                checkJavaIdentifier("argument type");
            }

            if (replaceSystemProperties)
            {
                nextWord = replaceSystemProperties(nextWord);
            }

            arguments.add(nextWord);

            // Read a comma (or a closing parenthesis, or a different word).
            readNextWord("separating '" + ConfigurationConstants.ARGUMENT_SEPARATOR_KEYWORD + "'" +
                         (endWithClosingParenthesis ?
                             " or closing '" + ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD + "'" :
                             ""));

            if (!ConfigurationConstants.ARGUMENT_SEPARATOR_KEYWORD.equals(nextWord))
            {
                if (endWithClosingParenthesis &&
                    !ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD.equals(nextWord))
                {
                    throw new ParseException("Expecting separating '" + ConfigurationConstants.ARGUMENT_SEPARATOR_KEYWORD +
                                             "' or closing '" + ConfigurationConstants.CLOSE_ARGUMENTS_KEYWORD +
                                             "' before " + reader.locationDescription());
                }

                break;
            }
        }

        return arguments;
    }


    /**
     * Throws a ParseException for an unexpected keyword.
     */
    private int unknownAccessFlag() throws ParseException
    {
        throw new ParseException("Unexpected keyword " + reader.locationDescription());
    }


    /**
     * Replaces any system properties in the given word by their values
     * (e.g. the substring "<java.home>" is replaced by its value).
     */
    private String replaceSystemProperties(String word)
    throws ParseException
    {
        int fromIndex = 0;
        while (true)
        {
            fromIndex = word.indexOf(ConfigurationConstants.OPEN_SYSTEM_PROPERTY, fromIndex);
            if (fromIndex < 0)
            {
                break;
            }

            int toIndex = word.indexOf(ConfigurationConstants.CLOSE_SYSTEM_PROPERTY, fromIndex+1);
            if (toIndex < 0)
            {
                throw new ParseException("Expecting closing '" + ConfigurationConstants.CLOSE_SYSTEM_PROPERTY +
                                         "' after opening '" + ConfigurationConstants.OPEN_SYSTEM_PROPERTY +
                                         "' in " + reader.locationDescription());
            }

            String propertyName  = word.substring(fromIndex+1, toIndex);
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue == null)
            {
                throw new ParseException("Value of system property '" + propertyName +
                                         "' is undefined in " + reader.locationDescription());
            }

            word = word.substring(0, fromIndex) +
                       propertyValue +
                       word.substring(toIndex+1);
        }

        return word;
    }


    /**
     * Reads the next word of the configuration in the 'nextWord' field,
     * throwing an exception if there is no next word.
     */
    private void readNextWord(String expectedDescription)
    throws ParseException, IOException
    {
        readNextWord();
        if (configurationEnd())
        {
            throw new ParseException("Expecting " + expectedDescription +
                                     " before " + reader.locationDescription());
        }
    }


    /**
     * Reads the next word of the configuration in the 'nextWord' field.
     */
    private void readNextWord()
    throws IOException
    {
        nextWord = reader.nextWord();
    }


    /**
     * Returns whether the end of the configuration has been reached.
     */
    private boolean configurationEnd()
    {
        return nextWord == null ||
               nextWord.startsWith(ConfigurationConstants.OPTION_PREFIX) ||
               nextWord.equals(ConfigurationConstants.AT_DIRECTIVE);
    }


    /**
     * Checks whether the given word is a valid Java identifier and throws
     * a ParseException if it isn't. Wildcard characters are accepted.
     */
    private void checkJavaIdentifier(String expectedDescription)
    throws ParseException
    {
        if (!isJavaIdentifier(nextWord))
        {
            throw new ParseException("Expecting " + expectedDescription +
                                     " before " + reader.locationDescription());
        }
    }


    /**
     * Returns whether the given word is a valid Java identifier.
     * Wildcard characters are accepted.
     */
    private boolean isJavaIdentifier(String aWord)
    {
        for (int index = 0; index < aWord.length(); index++)
        {
            char c = aWord.charAt(index);
            if (!(Character.isJavaIdentifierPart(c) ||
                  c == '.' ||
                  c == '[' ||
                  c == ']' ||
                  c == '<' ||
                  c == '>' ||
                  c == '*' ||
                  c == '?'))
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Checks whether the given access flags are valid field access flags,
     * throwing a ParseException if they aren't.
     */
    private void checkFieldAccessFlags(int requiredSetMemberAccessFlags,
                                       int requiredUnsetMemberAccessFlags)
    throws ParseException
    {
        if (((requiredSetMemberAccessFlags |
              requiredUnsetMemberAccessFlags) &
            ~ClassConstants.VALID_INTERNAL_ACC_FIELD) != 0)
        {
            throw new ParseException("Invalid method access modifier for field before " +
                                     reader.locationDescription());
        }
    }


    /**
     * Checks whether the given access flags are valid method access flags,
     * throwing a ParseException if they aren't.
     */
    private void checkMethodAccessFlags(int requiredSetMemberAccessFlags,
                                        int requiredUnsetMemberAccessFlags)
    throws ParseException
    {
        if (((requiredSetMemberAccessFlags |
              requiredUnsetMemberAccessFlags) &
            ~ClassConstants.VALID_INTERNAL_ACC_METHOD) != 0)
        {
            throw new ParseException("Invalid field access modifier for method before " +
                                     reader.locationDescription());
        }
    }


    /**
     * A main method for testing configuration parsing.
     */
    private static void main(String[] args) {
        try
        {
            ConfigurationParser parser = new ConfigurationParser(args);

            parser.parse(new Configuration());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
