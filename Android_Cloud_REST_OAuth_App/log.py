#Name: Adeline Harcourt
#Date: 7/14/2017
#Description: A simple REST URL structure for a Nutritional Log API. The single entity
#		is a "Log", which holds calories, weight, exercise, etc. for a particular date.
#		See URL_descriptions.pdf for more details.

from google.appengine.ext import ndb
from datetime import datetime
import webapp2
import json

# Define Log entity
class Log(ndb.Model):
	id = ndb.StringProperty()
	user = ndb.StringProperty(required=True)
	date = ndb.DateProperty(required=True)
	calories = ndb.IntegerProperty(required=True)
	exercise = ndb.BooleanProperty(default=False)
	weight = ndb.FloatProperty(required=True)

# Handle all requests made to the /log URL	
class LogHandler(webapp2.RequestHandler):
	def post(self):
		#Get JSON body
		log_data = json.loads(self.request.body)
		
		#Check required fields
		if ('date' not in log_data or ('calories' not in log_data or ('user' not in log_data or 'weight' not in log_data))):
			ErrorHandler(self, 400, "POST request must contain 'user', 'date', 'calories', and 'weight'")
			return
			
		#Format date
		log_date = datetime.strptime(log_data['date'], '%m-%d-%Y')

		#Create Log
		new_log = Log(user = log_data['user'], date = log_date, calories = log_data['calories'], exercise = log_data['exercise'], weight = log_data['weight'])

		#Add ID to log
		new_log.put()
		new_log.id = new_log.key.urlsafe()
		new_log.put()
		
		#Add self link to log
		log_dict = new_log.to_dict()
		log_dict['date'] = str(log_dict['date'])
		log_dict['self'] = '/logs/' + new_log.id
		
		#Respond with created log
		self.response.content_type = 'application/json'
		self.response.write(json.dumps(log_dict, indent=2))
		
	def put(self, id=None):
		if id:
			#Get JSON body
			log_data = json.loads(self.request.body)
			log = ndb.Key(urlsafe=id).get()
			
			#Update log values
			if 'date' in log_data:
				log_date = datetime.strptime(log_data['date'], '%m-%d-%Y')
				log.date = log_date
			if 'calories' in log_data:
				log.calories = log_data['calories']
			if 'exercise' in log_data:
				if log_data['exercise'] == True or log_data['exercise'] == False:
					log.exercise = log_data['exercise']
				else:
					ErrorHandler(self, 400, "exercise must be either 'False' or 'True'")
					return
			if 'weight' in log_data:
				log.weight = log_data['weight']
					
			log.put()
			log_d = log.to_dict()
			
			#Add self link to log
			log_d['date'] = str(log_d['date'])
			log_d['self'] = "/logs/" + id
			
			#Respond with updated log
			self.response.content_type = 'application/json'
			self.response.write(json.dumps(log_d, indent=2))
			
	def get(self, id=None):
		#If GET is for individual log, get by ID and respond
		if id:  
			log = ndb.Key(urlsafe=id).get()
			log_d = log.to_dict()
			log_d['date'] = str(log_d['date'])
			log_d['self'] = "/logs/" + id
			self.response.content_type = 'application/json'
			self.response.write(json.dumps(log_d, indent=2))
		
		#Otherwise, return all logs for user
		else:
			#If user was not specified, return error
			if ('user' not in self.request.headers):
				ErrorHandler(self, 400, "'user' must be submitted as a header with GET request")
				return
			
			#If date was passed as a query parameter, filter by the date and user
			if (self.request.get('date') != ''):
				q_date = datetime.strptime(self.request.get('date'), '%m-%d-%Y').date()
				self.response.write(q_date)
				user_name = self.request.headers['user']
				logs_dict = [logs.to_dict() for logs in Log.query(ndb.AND(Log.user == user_name, Log.date == q_date))]
				for logs in logs_dict:
					logs['date'] = str(logs['date'])
					logs['self'] = '/logs/' + logs['id']
				self.response.content_type = 'application/json'
				self.response.write(json.dumps(logs_dict, indent=2))
				
			#Otherwise, just filter by the user
			else:
				user_name = self.request.headers['user']
				logs_dict = [logs.to_dict() for logs in Log.query(Log.user == user_name).order(-Log.date)]
				for logs in logs_dict:
					logs['date'] = str(logs['date'])
					logs['self'] = '/logs/' + logs['id']
				self.response.content_type = 'application/json'
				self.response.write(json.dumps(logs_dict, indent=2))
	
	#Handle request to delete Log
	def delete(self, id=None):
		if id:
			log = ndb.Key(urlsafe=id).get()
			
			if log is not None:
				#Delete log
				log.key.delete()

#Handle errors
def ErrorHandler(object, status, message):
	object.response.status = status
	object.response.write(message)

class MainPage(webapp2.RequestHandler):
    def get(self):
        self.response.write("/logs")

allowed_methods = webapp2.WSGIApplication.allowed_methods
new_allowed_methods = allowed_methods.union(('PATCH',))
webapp2.WSGIApplication.allowed_methods = new_allowed_methods
app = webapp2.WSGIApplication([
    ('/', MainPage),
	('/logs', LogHandler),
	('/logs/(.*)', LogHandler)
], debug=True)
