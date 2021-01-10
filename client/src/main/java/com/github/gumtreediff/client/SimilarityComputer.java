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
    private static String toS(ITree node, TreeContext context) {
        return String.format("%s(%d)", node.toPrettyString(context), node.getId());
    }

    public static void main(String[] args) {
        if (!(args.length == 2 || args.length == 3 && args[2].equals("-v"))) {
            System.out.println("Usage: <some name> <file1> <file2> [-v]");
            return;
        }
        try {
            String srcFile = args[0];
            String dstFile = args[1];

            Run.initGenerators();
            TreeContext srcctx = Generators.getInstance().getTree(srcFile);
            Tree src = (Tree)(srcctx.getRoot());
            TreeContext dstctx = Generators.getInstance().getTree(dstFile);
            Tree dst = (Tree)(dstctx.getRoot());

            Matcher defaultMatcher = Matchers.getInstance().getMatcher(src, dst);
            defaultMatcher.match();
            Set<Mapping> mappingSet = defaultMatcher.getMappingsAsSet();

            if (args.length == 3) {
                System.out.println(TreeIoUtils.toLisp(srcctx).toString());
                System.out.println(TreeIoUtils.toLisp(dstctx).toString());
                for (Mapping m : mappingSet)
                    System.out.printf("Match %s to %s\n",
                        toS(m.first, srcctx), toS(m.second, dstctx));
            }

            int mappingSize = mappingSet.size();
            System.out.println((double)mappingSize / (src.getSize() + dst.getSize() - mappingSize));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}