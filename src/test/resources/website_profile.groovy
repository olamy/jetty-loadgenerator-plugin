//
//  ========================================================================
//  Copyright (c) 1995-2016 Webtide LLC, Olivier Lamy
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================

import org.mortbay.jetty.load.generator.Resource

return new Resource( "/index.html",
                             new Resource( "/style.css",
                               new Resource( "/logo.gif" ),
                               new Resource( "/spacer.png" )
                             ),
                             new Resource( "/fancy.css" ),
                             new Resource( "/script.js",
                                           new Resource( "/library.js" ),
                                           new Resource( "/morestuff.js" )
                            ),
                            new Resource( "/anotherScript.js" ),
                            new Resource( "/iframeContents.html" ),
                            new Resource( "/moreIframeContents.html" ),
                            new Resource( "/favicon.ico" )
);
