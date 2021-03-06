/*******************************************************************************
 * Copyright (c) 2013, Salesforce.com, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *     Neither the name of Salesforce.com nor the names of its contributors may 
 *     be used to endorse or promote products derived from this software without 
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.salesforce.phoenix.expression.function;

import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;
import com.salesforce.phoenix.expression.Expression;
import com.salesforce.phoenix.expression.LiteralExpression;
import com.salesforce.phoenix.schema.PDataType;

/**
 * 
 * Class encapsulating the CEIL operation on a {@link com.salesforce.phoenix.schema.PDataType#DECIMAL}
 *
 * @author samarth.jain
 * @since 3.0.0
 */
public class CeilDecimalExpression extends RoundDecimalExpression {
    
    public CeilDecimalExpression() {}
    
    public CeilDecimalExpression(List<Expression> children) {
        super(children);
    }
    
  /**
   * Creates a {@link CeilDecimalExpression} with rounding scale given by @param scale.
   *
   */
   public static Expression create(Expression expr, int scale) throws SQLException {
       if (expr.getDataType().isCoercibleTo(PDataType.LONG)) {
           return expr;
       }
       Expression scaleExpr = LiteralExpression.newConstant(scale, PDataType.INTEGER, true);
       List<Expression> expressions = Lists.newArrayList(expr, scaleExpr);
       return new CeilDecimalExpression(expressions);
   }
   
   /**
    * Creates a {@link CeilDecimalExpression} with a default scale of 0 used for rounding. 
    *
    */
   public static Expression create(Expression expr) throws SQLException {
       return create(expr, 0);
   }
    
    @Override
    protected RoundingMode getRoundingMode() {
        return RoundingMode.CEILING;
    }
    
    @Override
    public String getName() {
        return CeilFunction.NAME;
    }
    
}
