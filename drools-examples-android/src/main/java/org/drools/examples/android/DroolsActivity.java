package org.drools.examples.android;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import ch.qos.logback.classic.android.BasicLogcatConfigurator;
import org.drools.android.DroolsAndroidContext;
import org.drools.core.util.DroolsStreamUtils;
import org.drools.examples.helloworld.Message;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.rule.Rule;
import org.kie.api.definition.type.FactType;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.definition.KnowledgePackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DroolsActivity extends Activity {

    private static final Logger logger = LoggerFactory.getLogger(DroolsActivity.class);

    private KieBase mKnowledgeBase;
    private KieContainer mContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BasicLogcatConfigurator.configureDefaultContext();

        //Initialize android context and set system properties
        logger.info("os.name: " + System.getProperty("os.name"));
        Toast.makeText(this, "os.name: " + System.getProperty("os.name"), Toast.LENGTH_LONG).show();
        logger.info("java.version: " + System.getProperty("java.version"));
        System.setProperty("java.version", "1.6");
        DroolsAndroidContext.setContext(this);
        System.setProperty("drools.compiler", "JANINO");
        System.setProperty( "drools.dialect.java.compiler",
                "JANINO" );

        //load serialized KnowledgePackages from res/raw/helloworld.pkg
        new LoadRulesTask().execute(getResources().openRawResource(R.raw.org_drools_examples_helloworld));
        new LoadContainerTask().execute();

        RelativeLayout layout = new RelativeLayout(this);
        layout.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        Button button = new Button(this);
        button.setText("Fire Rules");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FireRulesTask().execute();
            }
        });
        layout.addView(button, lp);
        setContentView(layout);
    }

    private class LoadContainerTask extends AsyncTask<Void, Void, KieContainer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DroolsActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected KieContainer doInBackground(Void... params) {
            try {
                logger.debug("Loading Classpath container");
                KieServices ks = KieServices.Factory.get();
                return ks.getKieClasspathContainer();
            }catch(Exception e) {
                logger.error("Drools exception", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(KieContainer result) {
            mContainer = result;
            logger.info("THE CONTAINER: " + result);
            DroolsActivity.this.setProgressBarIndeterminateVisibility(false);
        }
    }


    private class LoadRulesTask extends AsyncTask<InputStream, Void, KieBase> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DroolsActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected KnowledgeBase doInBackground(InputStream... params) {
            try {
                logger.debug("Loading knowledge base");

                final KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
                List<KnowledgePackage> pkgs = new ArrayList<KnowledgePackage>();
                for(InputStream is : params) {
                    pkgs.add((KnowledgePackage) DroolsStreamUtils.streamIn(is));
                }
                kbase.addKnowledgePackages(pkgs);
                for(KnowledgePackage pkg : kbase.getKnowledgePackages()) {
                    logger.debug("Loaded rule package: " + pkg.toString());
                    for (Rule rule : pkg.getRules()) {
                        logger.debug("Rule: " + rule);
                    }
                }
                return kbase;
            }catch(Exception e) {
                logger.error("Drools exception", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(KieBase result) {
            mKnowledgeBase = result;
            DroolsActivity.this.setProgressBarIndeterminateVisibility(false);
        }
    }

    private class FireRulesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            logger.debug("Processing rules");
            DroolsActivity.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                logger.debug("Firing rules");
                final Message message = new Message();
                message.setMessage("Hello World");
                message.setStatus(Message.HELLO);
                FactType generated = mKnowledgeBase.getFactType("org.drools.examples.helloworld","TestType");
                Object obj = generated.newInstance();
                List<Object> list = new ArrayList<Object>();
                list.add(message);
                list.add(obj);


                final StatelessKieSession ksession = mKnowledgeBase.newStatelessKieSession();
                ksession.addEventListener(new DebugAgendaEventListener());
                ksession.addEventListener(new DebugRuleRuntimeEventListener());
                ksession.execute(list);

                final KieBase cBase = mContainer.getKieBase("HelloKB");
                logger.info("KBase: " + cBase);
                logger.info("KSessions: " + cBase.getKieSessions());

                final StatelessKieSession cSession = mContainer.newStatelessKieSession("android-session");
                cSession.execute(list);
            }catch(Exception e) {
                logger.error("Drools exception", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            DroolsActivity.this.setProgressBarIndeterminateVisibility(false);
        }
    }
}
