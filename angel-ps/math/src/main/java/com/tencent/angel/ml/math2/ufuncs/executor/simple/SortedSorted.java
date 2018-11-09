/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/Apache-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package com.tencent.angel.ml.math2.ufuncs.executor.simple;

import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.Scanner;

import java.io.StringReader;

public class SortedSorted {
  private volatile static StorageOperation executor;
  private static String inplaceOrNot =
		  "if (op.isInplace()){\n" +
				  "	 v1.setStorage(newStorage);\n" +
				  "  return v1;\n" +
				  "} else{\n" +
				  "	 return v;\n" +
				  "}\n";

  private static String union = "{\n" +
		  "Vector v = null;\n" +
		  "if (op.isKeepStorage()) {\n" +
		  "  v = CommonFuncs.getInstance().sortedVsSorted1(v1, v2, newStorage, op);\n" +
		  "} else {\n" +
		  "  v = CommonFuncs.getInstance().sortedVsSorted2(v1, v2, newStorage, op);\n" +
		  "}\n" +
		  inplaceOrNot +
		  "}\n\n";

  private static String intersection = "{\n" +
		  "Vector v = null;\n" +
		  "if (op.isKeepStorage()) {//sorted v2.size\n" +
		  "  v = CommonFuncs.getInstance().sortedVsSorted1(v1, v2, newStorage, op);\n" +
		  "} else {\n" +
		  "  v = CommonFuncs.getInstance().sortedVsSorted2(v1, v2, newStorage, op);\n" +
		  "}\n" +
		  inplaceOrNot +
		  "}\n\n";

  private static String all = "{\n" +
		  "Vector v = null;\n" +
		  "&dtype1Low[] resValues = newStorage.getValues();\n" +
		  "if (!op.isCompare()) {\n" +
		  "	for (int i = 0; i < resValues.length; i++) {\n" +
		  "		&replace;\n" +
		  "	}\n" +
		  "}\n" +
		  "if (op.isKeepStorage()) {\n" +
		  "	v = CommonFuncs.getInstance().sortedVsSorted1(v1, v2, newStorage, op);\n" +
		  "} else {\n" +
		  "	v = CommonFuncs.getInstance().sortedVsSorted2(v1, v2, newStorage, op);\n" +
		  "}\n" +
		  inplaceOrNot +
		  "}\n\n";

  public static StorageOperation getInstance() {
	if (executor == null) {
	  synchronized (StorageOperation.class) {
		if (executor == null) {
		  try {
			String[] itypes = new String[]{"Int", "Long"};
			String[] dtypes = new String[]{"Double", "Float", "Long", "Int"};
			String[] names = new String[]{"union", "intersection", "all"};
			String template = "import com.tencent.angel.ml.math2.ufuncs.executor.*;\n" +
					"import com.tencent.angel.ml.math2.storage.*;\n" +
					"import com.tencent.angel.ml.math2.ufuncs.expression.Binary;\n" +
					"import com.tencent.angel.ml.math2.utils.Constant;\n" +
					"import com.tencent.angel.ml.math2.vector.*;\n" +
					"import com.tencent.angel.ml.math2.utils.ArrayCopy;\n" +
					"import com.tencent.angel.ml.math2.exceptions.MathException;\n\n";
			String declTemplate = "public &ReturnType &Name(&Parameters)";
			String bodyTemplate = "&Declaration &Body";
			String declare, body, method, replace = "";
			for (int q = 0; q < names.length; q++) {
			  for (int i = 0; i < itypes.length; i++) {
				for (int j = 0; j < dtypes.length; j++) {
				  for (int p = j; p < dtypes.length; p++) {
					declare = declTemplate.replaceAll("&ReturnType", "Vector")
							.replaceAll("&Name", names[q])
							.replaceAll("&Parameters", "&itype&dtype1Vector v1, &itype&dtype2Vector v2" +
									", &itype&dtype1VectorStorage newStorage, Binary op");

					if (names[q] == "union") {
					  body = union;
					} else if (names[q] == "intersection") {
					  body = intersection;
					} else {
					  if (dtypes[j] == "Double" || dtypes[j] == "Float") {
						replace = " resValues[i] = &dtype1.NaN";
					  } else if (dtypes[j] == "Long") {
						replace = "throw new MathException(\"div by 0L!\")";
					  } else {
						replace = "throw new MathException(\"div by 0!\")";
					  }
					  body = all;
					}

					method = bodyTemplate.replaceAll("&Declaration", declare)
							.replaceAll("&Body", body)
							.replaceAll("&replace", replace)
							.replaceAll("&itypeLow", itypes[i].toLowerCase())
							.replaceAll("&dtype1Low", dtypes[j].toLowerCase())
							.replaceAll("&itype", itypes[i])
							.replaceAll("&dtype1", dtypes[j])
							.replaceAll("&dtype2", dtypes[p]);
					template += method;
				  }
				}
			  }
			}

			ClassBodyEvaluator ee = new ClassBodyEvaluator();
			executor = (StorageOperation) ee.createFastClassBodyEvaluator(
					new Scanner(null, new StringReader(
							template)),
					StorageOperation.class,
					(ClassLoader) null
			);
		  } catch (Exception e) {
			System.out.println(e);
		  }
		}
	  }

	}
	return executor;
  }
}