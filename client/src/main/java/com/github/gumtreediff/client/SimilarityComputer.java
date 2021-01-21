/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.client;

import java.util.*;
import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import com.github.gumtreediff.client.Run;

import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.ITree;

import com.github.gumtreediff.gen.Generators;

import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Mapping;

import com.github.gumtreediff.io.TreeIoUtils;

public class SimilarityComputer {
    private static Set<String> legal_suffixes = new HashSet<String>();
    private static String generator = null;

    private static String toS(ITree node, TreeContext context) {
        return String.format("%s(%d)", node.toPrettyString(context), node.getId());
    }

    private static void getTreeFromDir(File curDir, ITree node, TreeContext ctx) {
        File[] filesList = curDir.listFiles();
        Arrays.sort(filesList, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });
        for (File f : filesList) {
            if (f.isDirectory()) {
                ITree curNode = ctx.createTree(Tree.DIR_TYPE, "dir:" + f.getName(), Tree.DIR_TYPE_NAME);
                curNode.setParentAndUpdateChildren(node);

                getTreeFromDir(f, curNode, ctx);
            }
            if (f.isFile()
                    && legal_suffixes.contains(FilenameUtils.getExtension(f.getName()))) {
                ITree curNode = ctx.createTree(Tree.FILE_TYPE, "file:" + f.getName(), Tree.FILE_TYPE_NAME);
                curNode.setParentAndUpdateChildren(node);

                try {
                    TreeContext curCtx = Generators.getInstance().getTree(generator, f.getPath());
                    ITree curRoot = curCtx.getRoot();
                    curRoot.setParentAndUpdateChildren(curNode);
                    
                    ctx.merge(curCtx);
                } catch (IOException e) {
                    System.out.println("Error happens when generating AST from " + f.getPath());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args)
        throws UnsupportedOperationException, IOException {
        
        // parse options
        Options options = new Options();
        
        org.apache.commons.cli.Option langOpt =
                new org.apache.commons.cli.Option("l", "language", true,
                    "language of the project (c, c++, java, javascript, python)");
        langOpt.setRequired(true);
        options.addOption(langOpt);

        org.apache.commons.cli.Option verboseOpt =
                new org.apache.commons.cli.Option("v", false,
                    "verbose mode (for ASTs and mappings)");
        options.addOption(verboseOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, Arrays.copyOfRange(args, 0, args.length - 1));
        } catch (Exception e) {
            System.out.println("Error happens when parsing options...");
            e.printStackTrace();;
            formatter.printHelp("gumtree <options> path1 path2", options);
            System.exit(1);
        }

        // languages
        String language = cmd.getOptionValue("l").toLowerCase();
        if (language.equals("c")) {
            legal_suffixes.addAll(Arrays.asList("c", "h"));
            generator = "c-srcml";
        } else if (language.equals("c++") || language.equals("cpp")) {
            legal_suffixes.addAll(Arrays.asList("C", "CC", "cpp", "cc", "h", "hh", "hpp"));
            generator = "cpp-srcml";
        } else if (language.equals("java")) {
            legal_suffixes.addAll(Arrays.asList("java"));
            generator = "java-jdt";
        } else if (language.equals("javascript")) {
            legal_suffixes.addAll(Arrays.asList("js"));
            generator = "js-rhino";
        } else if (language.equals("python")) {
            legal_suffixes.addAll(Arrays.asList("py"));
            generator = "python-pythonparser";
        } else {
            System.out.println("Error happens when deciding languages...");
            formatter.printHelp("gumtree <options> path1 path2", options);
            System.exit(1);
        }

        // parse target arguments and build ASTs
        Run.initGenerators();
        File srcFile = new File(args[args.length - 2]);
        File dstFile = new File(args[args.length - 1]);
        TreeContext srcctx = new TreeContext();
        TreeContext dstctx = new TreeContext();
        if (srcFile.isFile()
                && legal_suffixes.contains(FilenameUtils.getExtension(srcFile.getName()))) {
            try {
                srcctx = Generators.getInstance().getTree(generator, srcFile.getPath());
            } catch (Exception e) {
                System.out.println("Error happens when generating AST from " + srcFile.getPath() + "...");
                e.printStackTrace();;
                formatter.printHelp("gumtree <options> path1 path2", options);
                System.exit(1);
            }
        } else if (srcFile.isDirectory()) {
            ITree node = srcctx.createTree(Tree.DIR_TYPE, "dir:" + srcFile.getPath(), Tree.DIR_TYPE_NAME);
            srcctx.setRoot(node);

            getTreeFromDir(srcFile, node, srcctx);
        } else {
            System.out.println("Error happens because of "
                    + srcFile.getPath() + " is neither a legal file nor a directory...");
            formatter.printHelp("gumtree <options> path1 path2", options);
            System.exit(1);
        }
        if (dstFile.isFile()
                && legal_suffixes.contains(FilenameUtils.getExtension(dstFile.getName()))) {
            try {
                dstctx = Generators.getInstance().getTree(generator, dstFile.getPath());
            } catch (Exception e) {
                System.out.println("Error happens when generating AST from " + dstFile.getPath() + "...");
                e.printStackTrace();;
                formatter.printHelp("gumtree <options> path1 path2", options);
                System.exit(1);
            }
        } else if (dstFile.isDirectory()) {
            ITree node = dstctx.createTree(Tree.DIR_TYPE, "dir:" + dstFile.getName(), Tree.DIR_TYPE_NAME);
            dstctx.setRoot(node);

            getTreeFromDir(dstFile, node, dstctx);
        } else {
            System.out.println("Error happens because of "
                    + dstFile.getPath() + " is neither a legal file nor a directory...");
            formatter.printHelp("gumtree <options> path1 path2", options);
            System.exit(1);
        }

        // calculate mappings between ASTs
        srcctx.validate();
        dstctx.validate();
        ITree src = srcctx.getRoot();
        ITree dst = dstctx.getRoot();

        Matcher defaultMatcher = Matchers.getInstance().getMatcher(src, dst);
        defaultMatcher.match();
        Set<Mapping> mappingSet = defaultMatcher.getMappingsAsSet();

        if (cmd.hasOption("v")) { // verbose mode
            System.out.println(TreeIoUtils.toLisp(srcctx).toString());
            System.out.println(TreeIoUtils.toLisp(dstctx).toString());
            for (Mapping m : mappingSet)
                System.out.printf("Match %s to %s\n",
                    toS(m.first, srcctx), toS(m.second, dstctx));
        }

        int mappingSize = mappingSet.size();
        System.out.println((double)mappingSize / (src.getSize() + dst.getSize() - mappingSize));
    }
}